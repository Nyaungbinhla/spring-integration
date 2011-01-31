/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.Message;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.groovy.GroovyCommandMessageProcessor;
import org.springframework.integration.groovy.ScriptVariablesGenerator;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.CustomizableThreadCreator;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a Groovy Script.
 * 
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class GroovyControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean {

	private volatile Long sendTimeout;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected MessageHandler createHandler() {
		ManagedBeansScriptVariableSource scriptVariableSource = new ManagedBeansScriptVariableSource(this.getBeanFactory());
		GroovyCommandMessageProcessor processor = new GroovyCommandMessageProcessor(scriptVariableSource);
		return this.configureHandler(new ServiceActivatingHandler(processor));
	}

	private ServiceActivatingHandler configureHandler(ServiceActivatingHandler handler) {
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}
	
	private class ManagedBeansScriptVariableSource implements ScriptVariablesGenerator {
		private final ListableBeanFactory beanFactory;
		
		public ManagedBeansScriptVariableSource(BeanFactory beanFactory){
			this.beanFactory = (beanFactory instanceof ListableBeanFactory) ? (ListableBeanFactory) beanFactory : null;
		}
		
		public Map<String, Object> generateScriptVariables(Message<?> message) {
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("headers", message.getHeaders());
			if (this.beanFactory != null){
				for (String name : this.beanFactory.getBeanDefinitionNames()) {
					Object bean = this.beanFactory.getBean(name);
					if (bean instanceof Lifecycle || 
						bean instanceof CustomizableThreadCreator || 
						(AnnotationUtils.findAnnotation(bean.getClass(), ManagedResource.class) != null)) {
							variables.put(name, bean);
					}
				}
			}
			return variables;
		}
	}
}
