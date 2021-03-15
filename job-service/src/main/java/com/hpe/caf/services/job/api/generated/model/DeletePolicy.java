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

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;



/**
 * The expiration details to be applied on the job
 **/

@ApiModel(description = "The expiration details to be applied on the job")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2021-03-12T12:28:51.784Z")
public class DeletePolicy   {

    private String expiryTime = null;


    public enum ExpirationOperationEnum {
        DELETE("delete");

        private String value;

        ExpirationOperationEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    private ExpirationOperationEnum expirationOperation = null;


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("expiryTime")
    public String getExpiryTime() {
        return expiryTime;
    }
    public void setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("expirationOperation")
    public ExpirationOperationEnum getExpirationOperation() {
        return expirationOperation;
    }
    public void setExpirationOperation(ExpirationOperationEnum expirationOperation) {
        this.expirationOperation = expirationOperation;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeletePolicy deletePolicy = (DeletePolicy) o;
        return Objects.equals(expiryTime, deletePolicy.expiryTime) &&
                Objects.equals(expirationOperation, deletePolicy.expirationOperation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiryTime, expirationOperation);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DeletePolicy {\n");

        sb.append("    expiryTime: ").append(toIndentedString(expiryTime)).append("\n");
        sb.append("    expirationOperation: ").append(toIndentedString(expirationOperation)).append("\n");
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
