/*
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

grammar Window;

@header {
//CHECKSTYLE:OFF
/*
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
}
COMMA : ',';
COLON : ':';

WINDOW : 'window' | 'windows';

INCLUDE : 'include' | 'INCLUDE' | 'includes' | 'INCLUDES' | 'including' | 'INCLUDING';
EXCLUDE : 'exclude' | 'EXCLUDE' | 'excludes' | 'EXCLUDES' | 'excluding' | 'EXCLUDING';

FROM : 'FROM' | 'from' | 'STARTING FROM' | 'starting from';
EVERY : 'EVERY' | 'every' | 'FOR EVERY' | 'for every';
TO : 'TO' | 'to' | 'until' | 'UNTIL';
AGO : 'AGO' | 'ago';

NUMBER : FIRST_DIGIT DIGIT*;
IDENTIFIER : [:][a-zA-Z0-9][a-zA-Z0-9_\.\-/]*;

DAY_SPECIFIER : MONDAY | TUESDAY | WEDNESDAY | THURSDAY
                       | FRIDAY | SATURDAY | SUNDAY
                       | CURRENT_DAY_OF_WEEK
                       | WEEKEND | WEEKDAY | HOLIDAYS
                       | DATE
                       ;

TIME_UNIT : SECOND_UNIT | MINUTE_UNIT | HOUR_UNIT | DAY_UNIT ;

WS : [ \r\t\u000C\n]+ -> skip;

fragment SECOND_UNIT : 'SECOND' | 'second' | 'seconds' | 'SECONDS' | 'second(s)' | 'SECOND(S)';
fragment MINUTE_UNIT : 'MINUTE' | 'minute' | 'minutes' | 'MINUTES' | 'minute(s)' | 'MINUTE(S)';
fragment HOUR_UNIT : 'HOUR' | 'hour' | 'hours' | 'HOURS' | 'hour(s)' | 'HOUR(S)';
fragment DAY_UNIT : 'DAY' | 'day' | 'days' | 'DAYS' | 'day(s)' | 'DAY(S)';
fragment MONDAY : 'MONDAY' | 'monday' | 'MONDAYS' | 'mondays';
fragment TUESDAY : 'TUESDAY' | 'tuesday' | 'TUESDAYS' | 'tuesdays';
fragment WEDNESDAY : 'WEDNESDAY' | 'wednesday' | 'WEDNESDAYS' | 'wednesdays';
fragment THURSDAY : 'THURSDAY' | 'thursday' | 'THURSDAYS' | 'thursdays';
fragment FRIDAY : 'FRIDAY' | 'friday' | 'FRIDAYS' | 'fridays';
fragment SATURDAY: 'SATURDAY' | 'saturday' | 'SATURDAYS' | 'saturdays';
fragment SUNDAY : 'SUNDAY' | 'sunday' | 'SUNDAYS' | 'sundays';
fragment CURRENT_DAY_OF_WEEK: 'this day of week' | 'THIS DAY OF WEEK' | 'this day of the week' | 'THIS DAY OF THE WEEK'
                            | 'current day of week' | 'CURRENT DAY OF WEEK'
                            | 'current day of the week' | 'CURRENT DAY OF THE WEEK';
fragment WEEKEND : 'weekend' | 'WEEKEND' | 'weekends' | 'WEEKENDS';
fragment WEEKDAY: 'weekday' | 'WEEKDAY' | 'weekdays' | 'WEEKDAYS';
fragment HOLIDAYS: 'holiday' | 'HOLIDAY' | 'holidays' | 'HOLIDAYS';
fragment DATE: 'date' | 'DATE';

fragment DIGIT : '0'..'9';
fragment FIRST_DIGIT : '1'..'9';

window : window_expression EOF;

window_expression : window_width including_specifier? excluding_specifier? #NonRepeatingWindow
                  | window_width skip_distance duration including_specifier? excluding_specifier? #RepeatingWindow
                  | duration #DenseWindow
                  ;

excluding_specifier : EXCLUDE specifier_list
                    ;
including_specifier : INCLUDE specifier_list
                    ;

specifier : day_specifier
          | day_specifier specifier_arg_list
          ;

specifier_arg_list : identifier
                   | identifier specifier_arg_list
                    ;

day_specifier : DAY_SPECIFIER ;

identifier : NUMBER | IDENTIFIER
          ;

specifier_list : specifier
               | specifier_list COMMA specifier
               ;

duration : FROM time_interval AGO? TO time_interval AGO? #FromToDuration
         | FROM time_interval AGO? #FromDuration
         ;

skip_distance : EVERY time_interval #SkipDistance
              ;

window_width : time_interval WINDOW? #WindowWidth
          ;

time_interval : time_amount time_unit #TimeInterval
              ;

time_amount : NUMBER #TimeAmount
            ;

time_unit : TIME_UNIT #TimeUnit
            ;
