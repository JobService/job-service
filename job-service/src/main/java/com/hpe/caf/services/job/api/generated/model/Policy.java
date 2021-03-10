/*
 * Copyright 2016-2021 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.services.job.api.generated.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Objects;


/**
 * The expiration details to be applied on the job
 **/

@ApiModel(description = "The expiration details to be applied on the job")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2021-03-10T17:25:43.851Z")
public class Policy   {

    private Date expiryTime = null;
    private Action action = null;


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("expiryTime")
    public Date getExpiryTime() {
        return expiryTime;
    }
    public void setExpiryTime(Date expiryTime) {
        this.expiryTime = expiryTime;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("action")
    public Action getAction() {
        return action;
    }
    public void setAction(Action action) {
        this.action = action;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Policy policy = (Policy) o;
        return Objects.equals(expiryTime, policy.expiryTime) &&
                Objects.equals(action, policy.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiryTime, action);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Policy {\n");

        sb.append("    expiryTime: ").append(toIndentedString(expiryTime)).append("\n");
        sb.append("    action: ").append(toIndentedString(action)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

