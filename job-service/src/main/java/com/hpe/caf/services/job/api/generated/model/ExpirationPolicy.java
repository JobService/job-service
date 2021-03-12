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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;



/**
 * The expiration policy to be applied on the job
 **/

@ApiModel(description = "The expiration policy to be applied on the job")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2021-03-12T12:28:51.784Z")
public class ExpirationPolicy   {

    private Policy active = null;
    private Policy completed = null;
    private Policy failed = null;
    private Policy cancelled = null;
    private Policy waiting = null;
    private Policy paused = null;
    private DeletePolicy expired = null;
    private Policy _default = null;


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Active")
    public Policy getActive() {
        return active;
    }
    public void setActive(Policy active) {
        this.active = active;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Completed")
    public Policy getCompleted() {
        return completed;
    }
    public void setCompleted(Policy completed) {
        this.completed = completed;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Failed")
    public Policy getFailed() {
        return failed;
    }
    public void setFailed(Policy failed) {
        this.failed = failed;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Cancelled")
    public Policy getCancelled() {
        return cancelled;
    }
    public void setCancelled(Policy cancelled) {
        this.cancelled = cancelled;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Waiting")
    public Policy getWaiting() {
        return waiting;
    }
    public void setWaiting(Policy waiting) {
        this.waiting = waiting;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Paused")
    public Policy getPaused() {
        return paused;
    }
    public void setPaused(Policy paused) {
        this.paused = paused;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Expired")
    public DeletePolicy getExpired() {
        return expired;
    }
    public void setExpired(DeletePolicy expired) {
        this.expired = expired;
    }


    /**
     **/

    @ApiModelProperty(value = "")
    @JsonProperty("Default")
    public Policy getDefault() {
        return _default;
    }
    public void setDefault(Policy _default) {
        this._default = _default;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpirationPolicy expirationPolicy = (ExpirationPolicy) o;
        return Objects.equals(active, expirationPolicy.active) &&
                Objects.equals(completed, expirationPolicy.completed) &&
                Objects.equals(failed, expirationPolicy.failed) &&
                Objects.equals(cancelled, expirationPolicy.cancelled) &&
                Objects.equals(waiting, expirationPolicy.waiting) &&
                Objects.equals(paused, expirationPolicy.paused) &&
                Objects.equals(expired, expirationPolicy.expired) &&
                Objects.equals(_default, expirationPolicy._default);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, completed, failed, cancelled, waiting, paused, expired, _default);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExpirationPolicy {\n");

        sb.append("    active: ").append(toIndentedString(active)).append("\n");
        sb.append("    completed: ").append(toIndentedString(completed)).append("\n");
        sb.append("    failed: ").append(toIndentedString(failed)).append("\n");
        sb.append("    cancelled: ").append(toIndentedString(cancelled)).append("\n");
        sb.append("    waiting: ").append(toIndentedString(waiting)).append("\n");
        sb.append("    paused: ").append(toIndentedString(paused)).append("\n");
        sb.append("    expired: ").append(toIndentedString(expired)).append("\n");
        sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
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
