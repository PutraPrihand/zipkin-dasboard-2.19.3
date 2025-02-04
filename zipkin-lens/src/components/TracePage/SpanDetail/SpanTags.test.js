/*
 * Copyright 2015-2020 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
import React from 'react';
import { createShallow } from '@material-ui/core/test-utils';

import SpanTags from './SpanTags';

describe('<SpanTags />', () => {
  let shallow;

  beforeEach(() => {
    shallow = createShallow();
  });

  it('should render all tags', () => {
    const wrapper = shallow(
      <SpanTags.Naked
        tags={[
          { key: 'key1', value: 'value1' },
          { key: 'key2', value: 'value2' },
          { key: 'key3', value: 'value3' },
        ]}
        classes={{}}
      />,
    );
    expect(wrapper.find('[data-testid="span-tags--table-row"]').length).toBe(3);
  });
});
