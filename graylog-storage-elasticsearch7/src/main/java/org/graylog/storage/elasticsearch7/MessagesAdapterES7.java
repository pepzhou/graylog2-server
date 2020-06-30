package org.graylog.storage.elasticsearch7;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Iterables;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.ElasticsearchException;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkItemResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.bulk.BulkResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.get.GetRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.get.GetResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.index.IndexRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.AnalyzeRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.indices.AnalyzeResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.rest.RestStatus;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.DocumentNotFoundException;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class MessagesAdapterES7 implements MessagesAdapter {
    private final ElasticsearchClient client;
    private final Meter invalidTimestampMeter;
    private final ChunkedBulkIndexer chunkedBulkIndexer;

    @Inject
    public MessagesAdapterES7(ElasticsearchClient elasticsearchClient, MetricRegistry metricRegistry, ChunkedBulkIndexer chunkedBulkIndexer) {
        this.client = elasticsearchClient;
        this.invalidTimestampMeter = metricRegistry.meter(name(Messages.class, "invalid-timestamps"));
        this.chunkedBulkIndexer = chunkedBulkIndexer;
    }

    @Override
    public ResultMessage get(String messageId, String index) throws IOException, DocumentNotFoundException {
        final GetRequest getRequest = new GetRequest(index, messageId);

        final GetResponse result = this.client.execute((c, requestOptions) -> c.get(getRequest, requestOptions));

        if (!result.isExists()) {
            throw new DocumentNotFoundException(index, messageId);
        }

        return ResultMessage.parseFromSource(messageId, index, result.getSource());
    }

    @Override
    public List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException {
        final AnalyzeRequest analyzeRequest = AnalyzeRequest.withIndexAnalyzer(index, analyzer, toAnalyze);

        final AnalyzeResponse result = client.execute((c, requestOptions) -> c.indices().analyze(analyzeRequest, requestOptions));
        return result.getTokens().stream()
                .map(AnalyzeResponse.AnalyzeToken::getTerm)
                .collect(Collectors.toList());
    }

    @Override
    public List<Messages.IndexingError> bulkIndex(List<IndexingRequest> messageList) {
        return chunkedBulkIndexer.index(messageList, this::bulkIndexChunked);
    }

    private List<Messages.IndexingError> bulkIndexChunked(ChunkedBulkIndexer.Chunk command) throws ChunkedBulkIndexer.EntityTooLargeException {
        final List<IndexingRequest> messageList = command.requests;
        final int offset = command.offset;
        final int chunkSize = command.size;

        if (messageList.isEmpty()) {
            return Collections.emptyList();
        }

        final Iterable<List<IndexingRequest>> chunks = Iterables.partition(messageList.subList(offset, messageList.size()), chunkSize);
        int chunkCount = 1;
        int indexedSuccessfully = 0;
        final List<Messages.IndexingError> indexFailures = new ArrayList<>();
        for (List<IndexingRequest> chunk : chunks) {

            final BulkRequest bulkRequest = new BulkRequest();
            chunk.forEach(request -> bulkRequest.add(
                    indexRequestFrom(request)
            ));

            final BulkResponse result;
            try {
                result = this.client.execute((c, requestOptions) -> c.bulk(bulkRequest, requestOptions));
            } catch (ElasticsearchException e) {
                for (ElasticsearchException cause : e.guessRootCauses()) {
                    if (cause.status().equals(RestStatus.REQUEST_ENTITY_TOO_LARGE)) {
                        throw new ChunkedBulkIndexer.EntityTooLargeException(indexedSuccessfully, indexingErrorsFrom(chunk));
                    }
                }
                throw e;
            }

            indexedSuccessfully += chunk.size();

            final List<BulkItemResponse> failures = Arrays.stream(result.getItems())
                    .filter(BulkItemResponse::isFailed)
                    .collect(Collectors.toList());
            indexFailures.addAll(indexingErrorsFrom(failures, messageList));
        }

        return indexFailures;
    }

    private List<Messages.IndexingError> indexingErrorsFrom(List<IndexingRequest> messageList) {
        return messageList.stream()
                .map(this::indexingErrorFrom)
                .collect(Collectors.toList());
    }

    private Messages.IndexingError indexingErrorFrom(IndexingRequest indexingRequest) {
        return Messages.IndexingError.create(indexingRequest.message(), indexingRequest.indexSet().getWriteIndexAlias());
    }

    private List<Messages.IndexingError> indexingErrorsFrom(List<BulkItemResponse> failedItems, List<IndexingRequest> messageList) {
        if (failedItems.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<String, Message> messageMap = messageList.stream()
                .map(IndexingRequest::message)
                .distinct()
                .collect(Collectors.toMap(Message::getId, Function.identity()));

        return failedItems.stream()
                .map(item -> {
                    final Message message = messageMap.get(item.getId());

                    return indexingErrorFromResponse(item, message);
                })
                .collect(Collectors.toList());
    }

    private Messages.IndexingError indexingErrorFromResponse(BulkItemResponse item, Message message) {
        return Messages.IndexingError.create(message, item.getIndex(), errorTypeFromResponse(item), item.getFailureMessage());
    }

    private Messages.IndexingError.ErrorType errorTypeFromResponse(BulkItemResponse item) {
        return Messages.IndexingError.ErrorType.Unknown;
    }

    private IndexRequest indexRequestFrom(IndexingRequest request) {
        return new IndexRequest(request.indexSet().getWriteIndexAlias())
                .id(request.message().getId())
                .source(request.message().toElasticSearchObject(this.invalidTimestampMeter));
    }
}