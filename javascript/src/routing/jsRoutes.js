const jsRoutes = {
  controllers: {
    api: {
      DashboardsApiController: {
        create: () => { return {url: '/dashboards' }; },
        index: () => { return {url: '/dashboards' }; },
        get: (id) => { return {url: '/dashboards/' + id }; },
        delete: (id) => { return {url: '/dashboards/' + id }; },
        update: (id) => { return {url: '/dashboards/' + id }; },
      },
      InputsApiController: {
        list: () => { return {url: '/system/inputs'}; },
        globalRecentMessage: (inputId) => { return {url: '/' + inputId}; },
      },
      OutputsApiController: {
        index: () => { return {url: '/system/outputs'}; },
        create: () => { return {url: '/system/outputs'}; },
        delete: (outputId) => { return {url: '/system/outputs/' + outputId}; },
        update: (outputId) => { return {url: '/system/outputs/' + outputId}; },
        availableType: (type) => { return {url: '/system/outputs/available/' + type}; },
        availableTypes: () => { return {url: '/system/outputs/available'}; },
      },
      RolesApiController: {
        listRoles: () => { return {url: '/roles'}; },
        createRole: () => { return {url: '/roles'}; },
        updateRole: (rolename) => { return {url: '/roles/' + rolename}; },
        deleteRole: (rolename) => { return {url: '/roles/' + rolename}; },
        loadMembers: (rolename) => { return {url: '/roles/' + rolename + '/members'}; },
      },
      StreamsApiController: {
        get: (streamId) => { return {url: '/streams/' + streamId}; },
        create: (streamId) => { return {url: '/streams'}; },
        update: (streamId) => { return {url: '/streams/' + streamId}; },
        cloneStream: (streamId) => { return {url: '/streams/' + streamId + '/clone'}; },
        delete: (streamId) => { return {url: '/streams/' + streamId}; },
        pause: (streamId) => { return {url: '/streams/' + streamId + '/pause'}; },
        resume: (streamId) => { return {url: '/streams/' + streamId + '/resume'}; },
      },
      StreamOutputsApiController: {
        add: (streamId, outputId) => { return {url: '/streams/' + streamId + '/outputs'}; },
        index: (streamId) => { return {url: '/streams/' + streamId + '/outputs'}; },
        delete: (streamId, outputId) => { return {url: '/streams/' + streamId + '/outputs/' + outputId}; },
      },
      StreamRulesApiController: {
        delete: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        update: (streamId, streamRuleId) => { return {url: '/streams/' + streamId + '/rules/' + streamRuleId}; },
        create: (streamId) => { return {url: '/streams/' + streamId + '/rules'}; },
      },
      SystemApiController: {
        fields: () => { return {url: '/system/fields'}; },
      }
    },
    DashboardsController: {
      show: (id) => { return {url: '/dashboards/' + id }; },
    },
  },
};

export default jsRoutes;
