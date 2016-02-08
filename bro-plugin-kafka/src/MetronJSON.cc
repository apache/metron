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
 
#include <threading/Formatter.h>
#include "MetronJSON.h"

using metron::formatter::MetronJSON;
using threading::formatter::JSON;
using threading::MsgThread;
using threading::Field;
using threading::Value;

MetronJSON::MetronJSON(string sn, MsgThread* t, TimeFormat tf)
    : JSON(t, tf)
    , stream_name(sn)
{
}

MetronJSON::~MetronJSON() {}

bool MetronJSON::Describe(ODesc* desc, int num_fields,
    const Field* const* fields, Value** vals) const
{
    desc->AddRaw("{");

    // prepend the stream name
    desc->AddRaw("\"");
    desc->AddRaw(stream_name);
    desc->AddRaw("\", ");

    // append the JSON formatted log record itself
    JSON::Describe(desc, num_fields, fields, vals);

    desc->AddRaw("}");
    return true;
}
