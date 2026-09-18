package cloud.cholewa.reporter.error;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(final Long taskId) {
        super("Task with id " + taskId + " does not exist");
    }
}
