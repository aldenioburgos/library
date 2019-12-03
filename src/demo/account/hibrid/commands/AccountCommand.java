package demo.account.hibrid.commands;

import java.io.Serializable;

public interface AccountCommand extends Serializable {

    int getSinteticValue(); //TODO encontrar um nome melhor para esse método.
}
