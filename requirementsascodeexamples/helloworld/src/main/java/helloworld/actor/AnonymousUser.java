package helloworld.actor;

import org.requirementsascode.AbstractActor;
import org.requirementsascode.Model;

import helloworld.command.EnterText;

public class AnonymousUser extends AbstractActor{
  private final AbstractActor helloWorldActor;
  private final String ageString;

  public AnonymousUser(AbstractActor helloWorldActor, String ageString) {
    this.helloWorldActor = helloWorldActor;
    this.ageString = ageString;
  }
  
  @Override
  public Model behavior() {
    Model model = Model.builder()
      .useCase("Get greeted")
        .basicFlow()
          .step("S1").systemPublish(() -> new EnterText(ageString)).to(helloWorldActor)
       .build();
    
    return model;
  }
}