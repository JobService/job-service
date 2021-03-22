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
import com.hpe.caf.services.job.api.generated.model.DeletePolicy.ExpirationOperationEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApiModel(
        description = "The expiration policy to be applied on the job"
)
public class ExpirationPolicy {
    private static final int EXPIRATION_AFTER_LAST_UPDATE = 0;
    private Policy active = null;
    private Policy completed = null;
    private Policy failed = null;
    private Policy cancelled = null;
    private Policy waiting = null;
    private Policy paused = null;
    private DeletePolicy expired = null;
    private Policy default_policy = null;

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

    @ApiModelProperty("")
    @JsonProperty("Cancelled")
    public Policy getCancelled() {
        return this.cancelled;
    }

    public void setCancelled(Policy cancelled) {
        this.cancelled = cancelled;
    }

    @ApiModelProperty("")
    @JsonProperty("Waiting")
    public Policy getWaiting() {
        return this.waiting;
    }

    public void setWaiting(Policy waiting) {
        this.waiting = waiting;
    }

    @ApiModelProperty("")
    @JsonProperty("Paused")
    public Policy getPaused() {
        return this.paused;
    }

    public void setPaused(Policy paused) {
        this.paused = paused;
    }

    @ApiModelProperty("")
    @JsonProperty("Expired")
    public DeletePolicy getExpired() {
        return this.expired;
    }

    public void setExpired(DeletePolicy expired) {
        this.expired = expired;
    }

    @ApiModelProperty("")
    @JsonProperty("Default")
    public Policy getDefault() {
        return this.default_policy;
    }

    public void setDefault(Policy _default) {
        this.default_policy = _default;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ExpirationPolicy expirationPolicy = (ExpirationPolicy)o;
            return Objects.equals(this.active, expirationPolicy.active) && Objects.equals(this.completed, expirationPolicy.completed) && Objects.equals(this.failed, expirationPolicy.failed) && Objects.equals(this.cancelled, expirationPolicy.cancelled) && Objects.equals(this.waiting, expirationPolicy.waiting) && Objects.equals(this.paused, expirationPolicy.paused) && Objects.equals(this.expired, expirationPolicy.expired) && Objects.equals(this.default_policy, expirationPolicy.default_policy);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.active, this.completed, this.failed, this.cancelled, this.waiting, this.paused, this.expired, this.default_policy});
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExpirationPolicy {\n");
        sb.append("    active: ").append(this.toIndentedString(this.active)).append("\n");
        sb.append("    completed: ").append(this.toIndentedString(this.completed)).append("\n");
        sb.append("    failed: ").append(this.toIndentedString(this.failed)).append("\n");
        sb.append("    cancelled: ").append(this.toIndentedString(this.cancelled)).append("\n");
        sb.append("    waiting: ").append(this.toIndentedString(this.waiting)).append("\n");
        sb.append("    paused: ").append(this.toIndentedString(this.paused)).append("\n");
        sb.append("    expired: ").append(this.toIndentedString(this.expired)).append("\n");
        sb.append("    _default: ").append(this.toIndentedString(this.default_policy)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        return o == null ? "null" : o.toString().replace("\n", "\n    ");
    }

    public List<String> toDBString() {
        List<String> policyList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        builDbExpirationPolicy(policyList, sb, "Active,", this.active);
        builDbExpirationPolicy(policyList, sb, "Completed,", this.completed);
        builDbExpirationPolicy(policyList, sb, "Failed,", this.failed);
        builDbExpirationPolicy(policyList, sb, "Cancelled,", this.cancelled);
        builDbExpirationPolicy(policyList, sb, "Waiting,", this.waiting);
        builDbExpirationPolicy(policyList, sb, "Paused,", this.paused);
        sb.setLength(0);
        sb.append("(");
        sb.append("Expired,")
                .append(this.toIndentedString(ExpirationOperationEnum.DELETE.toString())).append(",")
                .append(this.toIndentedString(this.expired.getExpiryTime())).append(",")
                .append(EXPIRATION_AFTER_LAST_UPDATE).append(",")
                .append(")");
        policyList.add(sb.toString());

        return policyList;
    }

    private void builDbExpirationPolicy(List<String> policyList, StringBuilder sb, String s, Policy active) {
        sb.setLength(0);
        sb.append("(");
        sb.append(s)
                .append(this.toIndentedString(active.getOperation().toString())).append(",")
                .append(this.toIndentedString(active.getExpiryTime())).append(",")
                .append(EXPIRATION_AFTER_LAST_UPDATE).append(",")
                .append(")");
        policyList.add(sb.toString());
    }
}
