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
package org.apache.metron.dataloads.bulk;

import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;


public abstract class DataPruner {

        protected Date startDate;
        protected Path wildcard;


        private DataPruner(){

        }

        public DataPruner(Date startDate, Integer numDays, String wildcard){

            this.startDate = dateAtMidnight(startDate);
            this.wildcard = new Path(wildcard);

        }


        protected Date dateAtMidnight(Date date) {

            Calendar calendar = Calendar.getInstance();

            calendar.setTime(date);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar.getTime();

        }

        abstract Long prune() throws IOException;
}
