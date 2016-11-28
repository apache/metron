/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.integration.processors;

import com.google.common.base.Function;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ProcessorResult;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.KafkaMessageSet;

import java.util.LinkedList;
import java.util.List;
public class KafkaProcessor<T> implements Processor<T> {
    private String kafkaComponentName;
    private String readTopic;
    private String errorTopic;
    private String invalidTopic;
    private List<byte[]> messages = new LinkedList<>();
    private List<byte[]> errors = new LinkedList<>();
    private List<byte[]> invalids = new LinkedList<>();
    private boolean readErrorsBefore = false;

    public KafkaProcessor(){}
    public KafkaProcessor withKafkaComponentName(String name){
        this.kafkaComponentName = name;
        return this;
    }
    public KafkaProcessor withReadTopic(String topicName){
        this.readTopic = topicName;
        return this;
    }
    public KafkaProcessor withErrorTopic(String topicName){
        this.errorTopic = topicName;
        return this;
    }
    public KafkaProcessor withInvalidTopic(String topicName){
        this.invalidTopic = topicName;
        return this;
    }
    public KafkaProcessor withValidateReadMessages(Function<KafkaMessageSet, Boolean> validate){
        this.validateReadMessages = validate;
        return this;
    }
    public KafkaProcessor withProvideResult(Function<KafkaMessageSet, T> provide){
        this.provideResult = provide;
        return this;
    }
    public KafkaProcessor withReadErrorsBefore(boolean flag){
        this.readErrorsBefore = flag;
        return this;
    }

    private Function<KafkaMessageSet, Boolean> validateReadMessages;
    private Function<KafkaMessageSet,T> provideResult;

    public ReadinessState process(ComponentRunner runner){
        KafkaComponent kafkaComponent = runner.getComponent(kafkaComponentName, KafkaComponent.class);
        messages.addAll(kafkaComponent.readMessages(readTopic));
        if(readErrorsBefore) {
            if (errorTopic != null) {
                errors.addAll(kafkaComponent.readMessages(errorTopic));
            }
            if (invalidTopic != null) {
                invalids.addAll(kafkaComponent.readMessages(invalidTopic));
            }
        }
        Boolean validated = validateReadMessages.apply(new KafkaMessageSet(messages,errors,invalids));
        if(validated == null){
            validated = false;
        }
        if(validated){
            return ReadinessState.READY;
        }else{
            if(!readErrorsBefore){
                if (errorTopic != null) {
                    errors.addAll(kafkaComponent.readMessages(errorTopic));
                }
                if (invalidTopic != null) {
                    invalids.addAll(kafkaComponent.readMessages(invalidTopic));
                }
            }
            if(errors.size() > 0 || invalids.size() > 0) {
                return ReadinessState.READY;
            }
            return ReadinessState.NOT_READY;
        }
    }
    @SuppressWarnings("unchecked")
    public ProcessorResult<T> getResult(){
        ProcessorResult.Builder<T> builder = new ProcessorResult.Builder();
        return builder.withResult(provideResult.apply(new KafkaMessageSet(messages,errors,invalids))).withProcessErrors(errors).withProcessInvalids(invalids).build();
    }
}


