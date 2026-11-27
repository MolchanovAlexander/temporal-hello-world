package helloworkflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface GreetActivities {
    // Greets the person identified by the supplied name.
    @ActivityMethod
    String greet(String name);

}
