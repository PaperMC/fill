/*
 * Copyright 2024 PaperMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.papermc.fill.configuration;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
@NullMarked
public class WebConfiguration {
  @Bean
  public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
    return new ShallowEtagHeaderFilter();
  }

  // https://github.com/spring-projects/spring-framework/issues/28552
  @Bean
  public FilterRegistrationBean<OncePerRequestFilter> trailingSlashUrlHandlerFilterRegistrationBean() {
    final FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
    final UrlHandlerFilter filter = UrlHandlerFilter
      .trailingSlashHandler("/v2/**").wrapRequest()
      .build();
    bean.setFilter(filter);
    return bean;
  }
}
