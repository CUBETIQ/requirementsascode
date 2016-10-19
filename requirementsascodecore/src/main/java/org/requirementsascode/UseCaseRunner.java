package org.requirementsascode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.requirementsascode.UseCaseStep.ActorPart;
import org.requirementsascode.exception.MissingUseCaseStepPartException;
import org.requirementsascode.exception.MoreThanOneStepCouldReactException;


/**
 * A use case runner is a highly configurable use case controller that received events
 * from the frontend and conditionally calls methods on the backend.
 * 
 * In requirementsascode, a use case runner is the only way the frontend communicates with the
 * backend. The use case runner is configured by the use case model it owns.
 * Each real user needs an instance of a use case runner, as the runner determines the journey
 * of the user through the use cases.
 * 
 *  
 */
public class UseCaseRunner {
	private List<Actor> actorsToRunAs;
	private UseCaseModel useCaseModel;
	private Optional<UseCaseStep> latestStep;
	private Optional<UseCaseFlow> latestFlow;
	private boolean isRunning;

	/**
	 * Constructor for creating a use case runner.
	 */
	public UseCaseRunner() {
		this.isRunning = false;
		this.useCaseModel = new UseCaseModel(this);
		as(useCaseModel.getUserActor()).restart();
	}
	
	/**
	 * Returns the use case model for the runner,
	 * which configures the behavior of the runner.
	 * 
	 * @return the use case model
	 */
	public UseCaseModel getUseCaseModel() {
		return useCaseModel;
	}
	
	/**
	 * Restarts the runner, setting latest flow ans latest step 
	 * to its original defaults ("no flow has been run, no step has been run").
	 */
	public void restart() {
		this.latestFlow = Optional.empty();
		this.latestStep = Optional.empty();
	}
	
	/**
	 * Runs the use case model. Calling this method triggers 
	 * "autonomous system reactions", and activates reacting to events
	 * via {@link #reactTo(Object)}.
	 * 
	 * @see UseCaseStep#system(Runnable)
	 * @return the runner itself, for method chaining, e.g. with {@link #as(Actor)}
	 */
	public UseCaseRunner run() {
		isRunning = true;
		triggerAutonomousSystemReaction();
		return this;
	}
	
	private void triggerAutonomousSystemReaction() {
		reactTo(new SystemEvent());
	}

	/**
	 * Defines the actor the runner is run as.
	 * 
	 * After calling this method, the runner will only trigger system reactions
	 * of steps that have explicitly set this actor, or that are "autonomous system reactions".
	 * 
	 * @see UseCaseStep#actor(Actor)
	 * @see UseCaseStep#system(Runnable) 
	 * @param actor the actor to run as
	 * @return the use case runner
	 */
	public UseCaseRunner as(Actor actor) {
		Objects.requireNonNull(actor);
		
		actorsToRunAs = Arrays.asList(actor, useCaseModel.getSystemActor());
		return this;
	}
	
	/**
	 * Needs to be called by the frontend to provide several event objects to the use case runner.
	 * For each event object, {@link #reactTo(Object)} is called.
	 * 
	 * @param events the events to react to
	 */
	public void reactTo(Object... events) {
		Objects.requireNonNull(events);
		
		for (Object event : events) {
			reactTo(event);
		}		
	}

	/**
	 * Needs to be called by the frontend to provide an event object to the use case runner.
	 * 
	 * The runner will then check which steps are enabled for the event. 
	 * If a single step is enabled, it will trigger the system reaction for that step.
	 * If no step is enabled, it will NOT trigger any system reaction.
	 * If more than one step is enabled, it will throw an exception.
	 * 
	 * After that, the runner will trigger "autonomous system reactions".
	 * 
	 * @see #getStepsEnabledFor(Class)
	 * @param <T> the type of the event object
	 * @param event the event object provided by the frontend
	 * @return the use case step that was enabled, or null if none was enabled.
	 * @throws MoreThanOneStepCouldReactException the exception that occurs if more than one step is enabled
	 */
	public <T> UseCaseStep reactTo(T event) {
		Objects.requireNonNull(event);
		
		UseCaseStep latestStepRun = null;
		if(isRunning){
			Class<? extends Object> currentEventClass = event.getClass();
			Set<UseCaseStep> reactingUseCaseSteps = getStepsEnabledFor(currentEventClass);
			latestStepRun = triggerSystemReaction(event, reactingUseCaseSteps);
		}
		return latestStepRun;
	}

	/**
	 * Returns the use case steps that are enabled for the specified event class.
	 * 
	 * A step is enabled if all of the following conditions are met:
	 * a) the step's actor matches the actor the runner is run an
	 * b) the step's event class is the same or a superclass of the specified event class 
	 * c) the condition of the step is fulfilled, that is: its predicate is true
	 * 
	 * @param eventClass the class of events
	 * @return the steps enabled for the class of events
	 */
	public Set<UseCaseStep> getStepsEnabledFor(Class<? extends Object> eventClass) {
		Objects.requireNonNull(eventClass);
		
		Stream<UseCaseStep> stepStream = useCaseModel.getUseCaseSteps().stream();
		Set<UseCaseStep> enabledSteps = getEnabledStepSubset(eventClass, stepStream);
		return enabledSteps;
	}
	
	Set<UseCaseStep> getEnabledStepSubset(Class<? extends Object> eventClass, Stream<UseCaseStep> stepStream) {
		Set<UseCaseStep> enabledSteps = stepStream
			.filter(step -> stepActorIsRunActor(step))
			.filter(step -> stepEventClassIsSameOrSuperclassAsEventClass(step, eventClass))
			.filter(step -> isConditionFulfilled(step))
			.collect(Collectors.toSet());
		return enabledSteps;
	}

	protected <T> UseCaseStep triggerSystemReaction(T event, Collection<UseCaseStep> useCaseSteps) {
		UseCaseStep useCaseStep = null;

		if(useCaseSteps.size() == 1){
			useCaseStep = useCaseSteps.iterator().next();
			triggerSystemReactionAndHandleException(event, useCaseStep);
		} else if(useCaseSteps.size() > 1){
			String message = getMoreThanOneStepCouldReactExceptionMessage(useCaseSteps);
			throw new MoreThanOneStepCouldReactException(message);
		}
		
		return useCaseStep;
	}
	protected <T> String getMoreThanOneStepCouldReactExceptionMessage(Collection<UseCaseStep> useCaseSteps) {
		String message = "System could react to more than one step: ";
		String useCaseStepsClassNames = useCaseSteps.stream().map(useCaseStep -> useCaseStep.toString())
				.collect(Collectors.joining(",", message, ""));
		return useCaseStepsClassNames;
	}
	
	protected <T> UseCaseStep triggerSystemReactionAndHandleException(T event, UseCaseStep useCaseStep) {
		if(useCaseStep.getSystemPart() == null){
			String message = getMissingSystemPartExceptionMessage(useCaseStep);
			throw new MissingUseCaseStepPartException(message);
		}
		
		setLatestStep(Optional.of(useCaseStep));
		setLatestFlow(Optional.of(useCaseStep.getUseCaseFlow()));
		
		try {
			@SuppressWarnings("unchecked")
			Consumer<T> systemReaction = 
				(Consumer<T>) useCaseStep.getSystemPart().getSystemReaction();
			triggerSystemReaction(event, systemReaction);
		} 
		catch (Exception e) { 
			handleException(e);
		} 
		
		triggerAutonomousSystemReaction();

		return useCaseStep;
	}
	protected String getMissingSystemPartExceptionMessage(UseCaseStep useCaseStep) {
		String message = "Use Case Step \"" + useCaseStep + "\" has no defined system part! Please have a look and update your Use Case Model for this step!";
		return message;
	}
	
	protected <T> void triggerSystemReaction(T event, Consumer<T> systemReaction) {
		systemReaction.accept(event);
	}

	protected void handleException(Exception e) {
		reactTo(e);
	}

	private boolean stepActorIsRunActor(UseCaseStep useCaseStep) {
		ActorPart actorPart = useCaseStep.getActorPart();
		if(actorPart == null){
			String message = getMissingActorPartExceptionMessage(useCaseStep);
			throw(new MissingUseCaseStepPartException(message));
		}
		
		return actorsToRunAs.contains(actorPart.getActor());
	}
	
	protected String getMissingActorPartExceptionMessage(UseCaseStep useCaseStep) {
		String message = "Use Case Step \"" + useCaseStep + "\" has no defined actor part! Please have a look and update your Use Case Model for this step!";
		return message;
	}
	
	private boolean stepEventClassIsSameOrSuperclassAsEventClass(UseCaseStep useCaseStep, Class<?> currentEventClass) {
		Class<?> stepEventClass = useCaseStep.getEventPart().getEventClass();
		return stepEventClass.isAssignableFrom(currentEventClass);
	}
	
	private boolean isConditionFulfilled(UseCaseStep useCaseStep) {
		Predicate<UseCaseRunner> predicate = useCaseStep.getPredicate();
		boolean result = predicate.test(this);
		return result;
	}
	
	public Optional<UseCaseStep> getLatestStep() {
		return latestStep;
	}
	
	public void setLatestStep(Optional<UseCaseStep> latestStep) {		
		this.latestStep = latestStep;
	}
	
	public Optional<UseCaseFlow> getLatestFlow() {
		return latestFlow;
	}
	
	public void setLatestFlow(Optional<UseCaseFlow> latestFlow) {
		this.latestFlow = latestFlow;
	}
}
