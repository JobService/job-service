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
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2021-02-09T15:07:30.523Z")
@XmlRootElement(name = "ExpirationPolicy")
public class ExpirationPolicy {
    private Policy active = null;
    private Policy completed = null;
    private Policy failed = null;

    public ExpirationPolicy() {
    }

    @ApiModelProperty("")
    @JsonProperty("Active")
    public Policy getActive() {
        return this.active;
    }

    public void setActive(Policy active) {
        this.active = active;
    }

    @ApiModelProperty("")
    @JsonProperty("Completed")
    public Policy getCompleted() {
        return this.completed;
    }

    public void setCompleted(Policy completed) {
        this.completed = completed;
    }

    @ApiModelProperty("")
    @JsonProperty("Failed")
    public Policy getFailed() {
        return this.failed;
    }

    public void setFailed(Policy failed) {
        this.failed = failed;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ExpirationPolicy expirationPolicy = (ExpirationPolicy)o;
            return Objects.equals(this.active, expirationPolicy.active) && Objects.equals(this.completed, expirationPolicy.completed) && Objects.equals(this.failed, expirationPolicy.failed);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.active, this.completed, this.failed});
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExpirationPolicy {\n");
        sb.append("    active: ").append(this.toIndentedString(this.active)).append("\n");
        sb.append("    completed: ").append(this.toIndentedString(this.completed)).append("\n");
        sb.append("    failed: ").append(this.toIndentedString(this.failed)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }
}

