/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.web.servlet;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ExposableControllerEndpoint;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ControllerEndpointHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ControllerEndpointHandlerMappingTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final StaticApplicationContext context = new StaticApplicationContext();

	@Test
	public void mappingWithNoPrefix() throws Exception {
		ExposableControllerEndpoint first = firstEndpoint();
		ExposableControllerEndpoint second = secondEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("", first, second);
		assertThat(mapping.getHandler(request("GET", "/first")).getHandler())
				.isEqualTo(handlerOf(first.getController(), "get"));
		assertThat(mapping.getHandler(request("POST", "/second")).getHandler())
				.isEqualTo(handlerOf(second.getController(), "save"));
		assertThat(mapping.getHandler(request("GET", "/third"))).isNull();
	}

	@Test
	public void mappingWithPrefix() throws Exception {
		ExposableControllerEndpoint first = firstEndpoint();
		ExposableControllerEndpoint second = secondEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", first,
				second);
		assertThat(mapping.getHandler(request("GET", "/actuator/first")).getHandler())
				.isEqualTo(handlerOf(first.getController(), "get"));
		assertThat(mapping.getHandler(request("POST", "/actuator/second")).getHandler())
				.isEqualTo(handlerOf(second.getController(), "save"));
		assertThat(mapping.getHandler(request("GET", "/first"))).isNull();
		assertThat(mapping.getHandler(request("GET", "/second"))).isNull();
	}

	@Test
	public void mappingNarrowedToMethod() throws Exception {
		ExposableControllerEndpoint first = firstEndpoint();
		ControllerEndpointHandlerMapping mapping = createMapping("actuator", first);
		this.thrown.expect(HttpRequestMethodNotSupportedException.class);
		mapping.getHandler(request("POST", "/actuator/first"));
	}

	private ControllerEndpointHandlerMapping createMapping(String prefix,
			ExposableControllerEndpoint... endpoints) {
		ControllerEndpointHandlerMapping mapping = new ControllerEndpointHandlerMapping(
				new EndpointMapping(prefix), Arrays.asList(endpoints), null);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		return mapping;
	}

	private HandlerMethod handlerOf(Object source, String methodName) {
		return new HandlerMethod(source,
				ReflectionUtils.findMethod(source.getClass(), methodName));
	}

	private MockHttpServletRequest request(String method, String requestURI) {
		return new MockHttpServletRequest(method, requestURI);
	}

	private ExposableControllerEndpoint firstEndpoint() {
		return mockEndpoint("first", new FirstTestMvcEndpoint());
	}

	private ExposableControllerEndpoint secondEndpoint() {
		return mockEndpoint("second", new SecondTestMvcEndpoint());
	}

	private ExposableControllerEndpoint mockEndpoint(String id, Object controller) {
		ExposableControllerEndpoint endpoint = mock(ExposableControllerEndpoint.class);
		given(endpoint.getId()).willReturn(id);
		given(endpoint.getController()).willReturn(controller);
		given(endpoint.getRootPath()).willReturn(id);
		return endpoint;
	}

	@ControllerEndpoint(id = "first")
	private static class FirstTestMvcEndpoint {

		@GetMapping("/")
		public String get() {
			return "test";
		}

	}

	@ControllerEndpoint(id = "second")
	private static class SecondTestMvcEndpoint {

		@PostMapping("/")
		public void save() {

		}

	}

}
