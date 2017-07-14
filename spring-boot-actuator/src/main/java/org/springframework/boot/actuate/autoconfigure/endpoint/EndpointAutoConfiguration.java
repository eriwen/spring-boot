/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.endpoint.AuditEventsEndpoint;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint;
import org.springframework.boot.actuate.endpoint.BeansEndpoint;
import org.springframework.boot.actuate.endpoint.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.FlywayEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.ThreadDumpEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for common management
 * {@link Endpoint}s.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Meang Akira Tanaka
 * @author Ben Hale
 */
@Configuration
@AutoConfigureAfter({ FlywayAutoConfiguration.class, LiquibaseAutoConfiguration.class })
@EnableConfigurationProperties(EndpointProperties.class)
public class EndpointAutoConfiguration {

	private final HealthAggregator healthAggregator;

	private final Map<String, HealthIndicator> healthIndicators;

	private final List<InfoContributor> infoContributors;

	private final Collection<PublicMetrics> publicMetrics;

	private final TraceRepository traceRepository;

	public EndpointAutoConfiguration(ObjectProvider<HealthAggregator> healthAggregator,
			ObjectProvider<Map<String, HealthIndicator>> healthIndicators,
			ObjectProvider<List<InfoContributor>> infoContributors,
			ObjectProvider<Collection<PublicMetrics>> publicMetrics,
			ObjectProvider<TraceRepository> traceRepository) {
		this.healthAggregator = healthAggregator.getIfAvailable();
		this.healthIndicators = healthIndicators.getIfAvailable();
		this.infoContributors = infoContributors.getIfAvailable();
		this.publicMetrics = publicMetrics.getIfAvailable();
		this.traceRepository = traceRepository.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public EnvironmentEndpoint environmentEndpoint(Environment environment) {
		return new EnvironmentEndpoint(environment);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public HealthEndpoint healthEndpoint() {
		return new HealthEndpoint(
				this.healthAggregator == null ? new OrderedHealthAggregator()
						: this.healthAggregator,
				this.healthIndicators == null
						? Collections.<String, HealthIndicator>emptyMap()
						: this.healthIndicators);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public BeansEndpoint beansEndpoint() {
		return new BeansEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public InfoEndpoint infoEndpoint() throws Exception {
		return new InfoEndpoint(this.infoContributors == null
				? Collections.<InfoContributor>emptyList() : this.infoContributors);
	}

	@Bean
	@ConditionalOnBean(LoggingSystem.class)
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public LoggersEndpoint loggersEndpoint(LoggingSystem loggingSystem) {
		return new LoggersEndpoint(loggingSystem);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public MetricsEndpoint metricsEndpoint() {
		List<PublicMetrics> publicMetrics = new ArrayList<>();
		if (this.publicMetrics != null) {
			publicMetrics.addAll(this.publicMetrics);
		}
		Collections.sort(publicMetrics, AnnotationAwareOrderComparator.INSTANCE);
		return new MetricsEndpoint(publicMetrics);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public TraceEndpoint traceEndpoint() {
		return new TraceEndpoint(this.traceRepository == null
				? new InMemoryTraceRepository() : this.traceRepository);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public ThreadDumpEndpoint dumpEndpoint() {
		return new ThreadDumpEndpoint();
	}

	@Bean
	@ConditionalOnBean(ConditionEvaluationReport.class)
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	@ConditionalOnEnabledEndpoint
	public AutoConfigurationReportEndpoint autoConfigurationReportEndpoint(
			ConditionEvaluationReport conditionEvaluationReport) {
		return new AutoConfigurationReportEndpoint(conditionEvaluationReport);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public ShutdownEndpoint shutdownEndpoint() {
		return new ShutdownEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint() {
		return new ConfigurationPropertiesReportEndpoint();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(AuditEventRepository.class)
	@ConditionalOnEnabledEndpoint
	public AuditEventsEndpoint auditEventsEndpoint(
			AuditEventRepository auditEventRepository) {
		return new AuditEventsEndpoint(auditEventRepository);
	}

	@Configuration
	@ConditionalOnBean(Flyway.class)
	@ConditionalOnClass(Flyway.class)
	static class FlywayEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledEndpoint
		public FlywayEndpoint flywayEndpoint(Map<String, Flyway> flyways) {
			return new FlywayEndpoint(flyways);
		}

	}

	@Configuration
	@ConditionalOnBean(SpringLiquibase.class)
	@ConditionalOnClass(SpringLiquibase.class)
	static class LiquibaseEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledEndpoint
		public LiquibaseEndpoint liquibaseEndpoint(
				Map<String, SpringLiquibase> liquibases) {
			return new LiquibaseEndpoint(liquibases);
		}

	}

	@Configuration
	@ConditionalOnClass(AbstractHandlerMethodMapping.class)
	protected static class RequestMappingEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RequestMappingEndpoint requestMappingEndpoint() {
			RequestMappingEndpoint endpoint = new RequestMappingEndpoint();
			return endpoint;
		}

	}

}
