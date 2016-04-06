/**
 * Created by CS on 05/04/2016.
 */
public class JobServiceTrackingInfoExpectation {

    /**
     * The job task identifier of the task under test.
     */
    private String jobTaskId;

    /**
     * The status check time
     */
    private String statusCheckTime;

    /**
     * The full URL address to access the isActive job service web method
     */
    private String statusCheckUrl;

    /**
     * The queue that job tracking worker is listening to
     */
    private String trackingPipe;

    /**
     * The final destination queue, when it is here the tracking info is removed
     */
    private String trackingTo;

    /**
     * Indicates whether we expect there to be TrackingInfo on the message forwarded by the Job Tracking Worker in the test.
     */
    private boolean trackingInfoPresent;

    public JobServiceTrackingInfoExpectation(String jobTaskId, String statusCheckTime, String statusCheckUrl, String trackingPipe, String trackingTo, boolean trackingInfoPresent) {
        this.jobTaskId = jobTaskId;
        this.statusCheckTime = statusCheckTime;
        this.statusCheckUrl = statusCheckUrl;
        this.trackingPipe = trackingPipe;
        this.trackingTo = trackingTo;
        this.trackingInfoPresent = trackingInfoPresent;
    }

    public String getJobTaskId() {
        return jobTaskId;
    }

    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = jobTaskId;
    }

    public String getStatusCheckTime() {
        return statusCheckTime;
    }

    public void setStatusCheckTime(String statusCheckTime) {
        this.statusCheckTime = statusCheckTime;
    }

    public String getStatusCheckUrl() {
        return statusCheckUrl;
    }

    public void setStatusCheckUrl(String statusCheckUrl) {
        this.statusCheckUrl = statusCheckUrl;
    }

    public String getTrackingPipe() {
        return trackingPipe;
    }

    public void setTrackingPipe(String trackingPipe) {
        this.trackingPipe = trackingPipe;
    }

    public String getTrackingTo() {
        return trackingTo;
    }

    public void setTrackingTo(String trackingTo) {
        this.trackingTo = trackingTo;
    }

    public boolean isTrackingInfoPresent() {
        return trackingInfoPresent;
    }

    public void setTrackingInfoPresent(boolean trackingInfoPresent) {
        this.trackingInfoPresent = trackingInfoPresent;
    }
}
