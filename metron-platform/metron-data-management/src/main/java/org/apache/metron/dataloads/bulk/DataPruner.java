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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public abstract class DataPruner {

    protected static final Logger LOG = LoggerFactory.getLogger(DataPruner.class);
    protected long firstTimeMillis;
    protected long lastTimeMillis;
    protected String wildCard;

    public DataPruner(Date startDate, Integer numDays, String wildCard) throws StartDateException {

        Date startAtMidnight = dateAtMidnight(startDate);
        this.lastTimeMillis = startDate.getTime();
        this.firstTimeMillis = lastTimeMillis - TimeUnit.DAYS.toMillis(numDays);
        this.wildCard = wildCard;

        Date today = dateAtMidnight(new Date());

        if (!today.after(startAtMidnight)) {
            throw new StartDateException("Prune Start Date must be prior to today");
        }
    }

    protected Date dateAtMidnight(Date date) {

        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();

    }


    public abstract Long prune() throws IOException;


}
