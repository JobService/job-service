/*
 * Copyright 2016-2023 Open Text.
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
import io.swagger.annotations.ApiModelProperty;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.Objects;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-03-03T15:07:30.523Z")
@XmlRootElement(name = "Failure")
public class Failure   {

    private String failureId = null;
    private Date failureTime = null;
    private String failureSource = null;
    private String failureMessage = null;

    /**
     **/
    public Failure failureId(String failureId) {
        this.failureId = failureId;
        return this;
    }


    @ApiModelProperty(value = "")
    @JsonProperty("failureId")
    public String getFailureId() {
        return failureId;
    }
    public void setFailureId(String failureId) {
        this.failureId = failureId;
    }


    /**
     **/
    public Failure failureTime(Date failureTime) {
        this.failureTime = failureTime;
        return this;
    }


    @ApiModelProperty(value = "")
    @JsonProperty("failureTime")
    public Date getFailureTime() {
        return failureTime;
    }
    public void setFailureTime(Date failureTime) {
        this.failureTime = failureTime;
    }

    /**
     **/
    public Failure failureSource(String failureSource) {
        this.failureSource = failureSource;
        return this;
    }


    @ApiModelProperty(value = "")
    @JsonProperty("failureSource")
    public String getFailureSource() {
        return failureSource;
    }
    public void setFailureSource(String failureSource) {
        this.failureSource = failureSource;
    }

    /**
     **/
    public Failure failureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
        return this;
    }


    @ApiModelProperty(value = "")
    @JsonProperty("failureMessage")
    public String getFailureMessage() {
        return failureMessage;
    }
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Failure failure = (Failure) o;
        return Objects.equals(failureId, failure.failureId) &&
                Objects.equals(failureSource, failure.failureSource) &&
                Objects.equals(failureTime, failure.failureTime) &&
                Objects.equals(failureMessage, failure.failureMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(failureId, failureTime, failureSource, failureMessage);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Failure {\n");

        sb.append("    failureId: ").append(toIndentedString(failureId)).append("\n");
        sb.append("    failureSource: ").append(toIndentedString(failureSource)).append("\n");
        sb.append("    failureTime: ").append(toIndentedString(failureTime)).append("\n");
        sb.append("    failureMessage: ").append(toIndentedString(failureMessage)).append("\n");
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

