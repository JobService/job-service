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
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2021-02-09T15:07:30.523Z")
@XmlRootElement(name = "Policy")
public class Policy {
    private Policy.ReferenceDateEnum referenceDate = null;
    private Integer numberOfDays = null;

    public Policy() {
    }

    @ApiModelProperty("")
    @JsonProperty("referenceDate")
    public Policy.ReferenceDateEnum getReferenceDate() {
        return this.referenceDate;
    }

    public void setReferenceDate(Policy.ReferenceDateEnum referenceDate) {
        this.referenceDate = referenceDate;
    }

    @ApiModelProperty("")
    @JsonProperty("numberOfDays")
    public Integer getNumberOfDays() {
        return this.numberOfDays;
    }

    public void setNumberOfDays(Integer numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Policy policy = (Policy)o;
            return Objects.equals(this.referenceDate, policy.referenceDate) && Objects.equals(this.numberOfDays, policy.numberOfDays);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.referenceDate, this.numberOfDays});
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Policy {\n");
        sb.append("    referenceDate: ").append(this.toIndentedString(this.referenceDate)).append("\n");
        sb.append("    numberOfDays: ").append(this.toIndentedString(this.numberOfDays)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }

    public static enum ReferenceDateEnum {
        CREATEDATE("createDate"),
        LASTUPDATEDATE("lastUpdatedate");

        private String value;

        private ReferenceDateEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String toString() {
            return this.value;
        }
    }
}
