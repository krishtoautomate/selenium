// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote.service;

import java.io.File;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.manager.SeleniumManagerOutput.Result;
import org.openqa.selenium.remote.NoSuchDriverException;

public class DriverFinder {

  public static Result getPath(DriverService service, Capabilities options) {
    return getPath(service, options, false);
  }

  public static Result getPath(DriverService service, Capabilities options, boolean offline) {
    Require.nonNull("Browser options", options);
    Result result = new Result(System.getProperty(service.getDriverProperty()));

    if (result.getDriverPath() == null) {
      try {
        result = SeleniumManager.getInstance().getDriverPath(options, offline);
      } catch (RuntimeException e) {
        throw new WebDriverException(
            String.format("Unable to obtain: %s, error %s", options, e.getMessage()), e);
      }
    }

    String message;
    if (result.getDriverPath() == null) {
      message = String.format("Unable to locate or obtain %s", service.getDriverName());
    } else if (!new File(result.getDriverPath()).exists()) {
      message =
          String.format(
              "%s located at %s, but invalid", service.getDriverName(), result.getDriverPath());
    } else if (!new File(result.getDriverPath()).canExecute()) {
      message =
          String.format(
              "%s located at %s, cannot be executed",
              service.getDriverName(), result.getDriverPath());
    } else {
      return result;
    }

    throw new NoSuchDriverException(message);
  }
}
