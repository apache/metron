/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Plugin.h"
#include "KafkaWriter.h"

namespace plugin { namespace Bro_Kafka {
    Plugin plugin;
}}

using namespace plugin::Bro_Kafka;

plugin::Configuration Plugin::Configure()
{
    AddComponent(new ::logging::Component("KafkaWriter", ::logging::writer::KafkaWriter::Instantiate));

    plugin::Configuration config;
    config.name = "Bro::Kafka";
    config.description = "Writes logs to Kafka";
    config.version.major = 0;
    config.version.minor = 1;
    return config;
}
