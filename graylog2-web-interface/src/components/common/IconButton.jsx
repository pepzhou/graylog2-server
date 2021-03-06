// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import Icon from 'components/common/Icon';

const Wrapper: StyledComponent<{}, ThemeInterface, HTMLButtonElement> = styled.button(({ theme }) => `
  display: inline-flex;
  justify-content: center;
  align-items: center;

  height: 25px;
  width: 25px;
  border: 0

  background-color: transparent;
  cursor: pointer;
  color: ${theme.colors.gray[70]};
  font-size: ${theme.fonts.size.large};

  :hover {
    background-color: ${theme.colors.gray[90]};
  }

  :active {
    background-color: ${theme.colors.gray[80]};
  }
`);

type Props = {
  focusable?: boolean,
  title: string,
  onClick?: () => void,
};

const handleClick = (onClick) => {
  if (typeof onClick === 'function') {
    onClick();
  }
};

const IconButton = ({ title, onClick, focusable, ...rest }: Props) => (
  <Wrapper tabIndex={focusable ? 0 : -1} title={title} onClick={() => handleClick(onClick)}>
    <Icon {...rest} />
  </Wrapper>
);

IconButton.propTypes = {
  title: PropTypes.string,
  onClick: PropTypes.func,
};

IconButton.defaultProps = {
  focusable: true,
  onClick: undefined,
  title: undefined,
};

export default IconButton;
