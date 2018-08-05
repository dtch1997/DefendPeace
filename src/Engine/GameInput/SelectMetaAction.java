package Engine.GameInput;

public class SelectMetaAction extends GameInputState
{
  private enum MetaAction
  {
    CO_INFO, CO_ABILITY, QUIT_GAME, END_TURN
  }
  private static final MetaAction[] NO_ABILITY = {MetaAction.CO_INFO, MetaAction.QUIT_GAME, MetaAction.END_TURN};

  public SelectMetaAction(StateData data)
  {
    super(data);
  }

  @Override
  protected OptionSet initOptions()
  {
    OptionSet metaActions = null;
    if( !myStateData.commander.getReadyAbilities().isEmpty() )
    {
      metaActions = new OptionSet(MetaAction.values());
    }
    else
    {
      // No CO_Ability available.
      metaActions = new OptionSet(NO_ABILITY);
    }

    return metaActions;
  }

  @Override
  public GameInputState select(Object option)
  {
    GameInputState next = this;
    if( MetaAction.CO_INFO == option )
    {
      
    }
    else if( MetaAction.QUIT_GAME == option )
    {
      next = new ConfirmExit(myStateData);
    }
    else if( MetaAction.CO_ABILITY == option )
    {
      
    }
    else if( MetaAction.END_TURN == option )
    {
      next = new StartNextTurn(myStateData);
    }
    return next;
  }
}
