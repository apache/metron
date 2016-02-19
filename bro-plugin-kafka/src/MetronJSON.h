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
 
#ifndef BRO_PLUGIN_METRON_KAFKA_METRONJSON_H
#define BRO_PLUGIN_METRON_KAFKA_METRONJSON_H

#include <string>
#include <threading/Formatter.h>
#include <threading/formatters/JSON.h>

using threading::Field;
using threading::Value;
using threading::formatter::JSON;

namespace metron {
namespace formatter {

 /**
  * A formatter that produces bro records in a format accepted by
  * Metron. Specifically, the stream ID is prepended to each JSON
  * formatted log record.
  *
  * {"conn": { ... }}
  * {"dns":  { ... }}
  */
    class MetronJSON : public JSON {

    public:
        MetronJSON(string stream_name, threading::MsgThread* t, TimeFormat tf);
        virtual ~MetronJSON();
        virtual bool Describe(ODesc* desc, int num_fields, const Field* const* fields,
            Value** vals) const;

    private:
        string stream_name;
    };
}
}

#endif
