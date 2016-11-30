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
export class MetronAlerts {

  public static SUCESS_MESSAGE_DISPALY_TIME = 5000;

  private createMessage(message: string, type: string): Node {
    let element = document.createElement('div');
    element.id = 'alert_placeholder';
    element.style.zIndex = '1000';
    element.style.position = 'fixed';
    element.style.display = 'inline-block';
    element.style.minWidth = '250px';
    element.style.cssFloat = 'right';
    element.style.top = '5px';
    element.style.right = '35px';

    element.innerHTML = '<div id="alertdiv" class="alert ' + type + '"><a class="close" data-dismiss="alert">×</a><span>' +
      message + '</span></div>';
    document.body.appendChild(element);

    return element;
  }

  showErrorMessage(message: string): void {
    this.createMessage(message, 'alert-danger');
  }

  showSuccessMessage(message: string): void {
    let element = this.createMessage(message, 'alert-success');
    setTimeout(function () {

      document.body.removeChild(element);

    }, MetronAlerts.SUCESS_MESSAGE_DISPALY_TIME);
  }
}
