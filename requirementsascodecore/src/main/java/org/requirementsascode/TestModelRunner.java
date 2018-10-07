package org.requirementsascode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple model runner for automated tests.
 *
 * @author b_muth
 */
public class TestModelRunner extends ModelRunner {
    private static final long serialVersionUID = 1161211712610795119L;

    private List<String> recordedStepNames;
    private List<Object> recordedEvents;

    private Consumer<StandardEventHandler> adaptedEventHandler;

    public TestModelRunner() {
	recordedStepNames = new ArrayList<>();
	recordedEvents = new ArrayList<>();
	super.handleWith(this::stepNameTracking);
    }

    /**
     * Returns the recorded names of the steps that have been run so far.
     * <p>
     * If no step has been run, an empty array is returned. For example, this method
     * can used with the assertArrayEquals method of JUnit to compare the actual
     * names of steps that have been run (returned by this method) to the expected
     * step names.
     *
     * @return the ordered names of steps run by this runner
     */
    public String[] getRecordedStepNames() {
	String[] stepNames = recordedStepNames.stream().toArray(String[]::new);
	return stepNames;
    }

    /**
     * Returns the recorded events that the runner reacted to so far.
     * <p>
     * If no events have caused a system reaction, an empty array is returned. For
     * example, this method can used with the assertArrayEquals method of JUnit to
     * compare the actual events that caused a reaction (returned by this method) to
     * the expected events.
     *
     * @return the ordered events that caused a system reaction
     */
    public Object[] getRecordedEvents() {
	Object[] events = recordedEvents.toArray();
	return events;
    }

    @Override
    public void handleWith(Consumer<StandardEventHandler> adaptedEventHandler) {
	this.adaptedEventHandler = adaptedEventHandler;
    }

    private void stepNameTracking(StandardEventHandler standardEventHandler) {
	recordedStepNames.add(standardEventHandler.getStepName());
	standardEventHandler.getEvent().ifPresent(event -> recordedEvents.add(event));

	if (adaptedEventHandler == null) {
	    standardEventHandler.handleEvent();
	} else {
	    adaptedEventHandler.accept(standardEventHandler);
	}
    }
}
