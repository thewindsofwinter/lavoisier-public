import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;

public class Lavoisier {
    public static void main(String[] args) throws LoginException, InterruptedException {
        JDA lavoisier = JDABuilder.createDefault("[INSERT TOKEN HERE]").build();

        // Basic functionality
        lavoisier.awaitReady().addEventListener(new LavoisierListener(lavoisier));
        lavoisier.awaitReady().getPresence().setActivity(Activity.watching("over you all! $help"));
    }
}
