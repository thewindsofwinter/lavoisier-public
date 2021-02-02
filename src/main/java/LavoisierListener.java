import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Random;

public class LavoisierListener extends ListenerAdapter {

    private static final String BOT_VERSION = "Lavoisier v0.2";
    private static final String ICON_URL = "https://cdn.discordapp.com/attachments/536299955178962965/771746782190764032/science-2652279_1280.png";
    private static final char PREFIX = '$';
    private static final int MESSAGE_COLOR = new Color(0, 200, 200).getRGB();
    private static final MessageEmbed.Footer FOOTER = new MessageEmbed.Footer(BOT_VERSION, ICON_URL, null);
    private static final MessageEmbed.AuthorInfo AUTHOR_INFO = new MessageEmbed.AuthorInfo("Lavoisier - a bot for all things USNCO", null, null, null);
    private Random rng = new Random();
    private static final int[] ERROR_CODE = {-1, -1};


    private int[] setYearRestriction(Message msg, boolean yearRestriction, String parameter) {
        if(yearRestriction) {
            MessageChannel ch = msg.getChannel();
            ch.sendMessage("`Error: Invalid query [tried to set -y twice]. Query terminated.`").queue();
            return ERROR_CODE;
        }

        if(!parameter.matches("[0-9]{4}-[0-9]{4}") && !parameter.matches("[0-9]{4}")) {
            MessageChannel ch = msg.getChannel();
            ch.sendMessage("`Error: Invalid query [year ranges in incorrect format]. Query terminated.`").queue();
            return ERROR_CODE;
        }
        else {
            String[] years = parameter.split("-");
            int lowerYear = Integer.parseInt(years[0]);
            int upperYear = Integer.parseInt(years[0]);

            if(years.length > 1) {
                upperYear = Integer.parseInt(years[1]);
            }

            if(lowerYear > upperYear) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [year range invalid]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            if(lowerYear < 1999 || upperYear > 2020) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [years not between 1999-2020]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            return new int[]{lowerYear, upperYear};
        }
    }


    private int[] setProblemRestriction(Message msg, boolean problemRestriction, String parameter, boolean partTwo) {
        int maxProblem = 60;
        if(partTwo) {
            // If it's part II
            maxProblem = 8;
        }

        if(problemRestriction) {
            MessageChannel ch = msg.getChannel();
            ch.sendMessage("`Error: Invalid query [tried to set -p twice]. Query terminated.`").queue();
            return ERROR_CODE;
        }

        if(!parameter.matches("[0-9]{1,2}-[0-9]{1,2}") && !parameter.matches("[0-9]{1,2}")) {
            MessageChannel ch = msg.getChannel();
            ch.sendMessage("`Error: Invalid query [problem ranges in incorrect format]. Query terminated.`").queue();
            return ERROR_CODE;
        }
        else {
            String[] problems = parameter.split("-");
            int lowerProblem = Integer.parseInt(problems[0]);
            int upperProblem = Integer.parseInt(problems[0]);

            if(problems.length > 1) {
                upperProblem = Integer.parseInt(problems[1]);
            }

            if(lowerProblem > upperProblem) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [problem range invalid]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            if(lowerProblem < 1 || upperProblem > maxProblem) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [problem not between 1-" + maxProblem + "]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            return new int[]{lowerProblem, upperProblem};
        }
    }

    private void printHelpMessage(Message msg, String[] parameters) {
        // No parameters
        if(parameters.length == 1) {
            MessageEmbed embed = new MessageEmbed(null, "*Current Commands*",
                    "**$help:** Display the help page for all commands." +
                            "\n\n**$query [-s PART_CODE] [-y YEARS] [-p PROBLEMS]**: Query a USNCO Locals " +
                            "or Nationals problem. A part code, year range, or problem range is optional. For more " +
                            "information, type `$help query`", null,
                    msg.getTimeCreated().plusSeconds(5), MESSAGE_COLOR, null, null,
                    AUTHOR_INFO, null, FOOTER, null, null);

            MessageChannel ch = msg.getChannel();
            ch.sendMessage(embed).queue();
        }
        else {
            String command = parameters[1];

            if(!command.equals("query")) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("The command you queried either does not have a" +
                        "help page or does not exist. For general help, type $help.").queue();
            }
            else {
                MessageEmbed embed = new MessageEmbed(null, "*Query Problems*",
                        "**query: ** `query [-s PART_CODE] [-y YEARS] [-p PROBLEMS]`" +
                                "\n\nQuery a USNCO Locals or Nationals problem." +
                                "\n\n" +
                                "-s, -section __part code__: " +
                                "\n\tOptional, restricts the part of the USNCO problems" +
                                "\n\tqueried. Can be either 0, 1, or 2. See explanation below:" +
                                "\n\t\t * Code 0: Locals (Part 0 like pre-nationals)" +
                                "\n\t\t * Code 1: Nationals Part I" +
                                "\n\t\t * Code 2: Nationals Part II" +
                                "\n\nDefault queries only query Locals and Nationals Part I, " +
                                "\n\tMake sure that -s comes before -y or -p." +
                                "\n\n" +
                                "-y, -year __year range__: " +
                                "\n\tOptional, restricts the year range. Year range should" +
                                "\n\tbe formatted as either a single year or two years with" +
                                "\n\ta dash in between (i.e. 2000 or 2003-2007). Inclusive." +
                                "\n\n" +
                                "-p, -problem __problem range__: " +
                                "\n\tOptional, restricts the problem range. Problem range" +
                                "\n\tshould be formatted as either a single problem number" +
                                "\n\tor two problem numbers with a dash in between " +
                                "\n\t(i.e. 60 or 12-17). Inclusive." +
                                "\n\nNote that part 2 values should be between 1-8" +
                                "\n\n" +
                                "**Usage Examples**" +
                                "\n\n" +
                                "`$query`: grabs a random problem from locals/nationals part I.\n" +
                                "`$query -s 2:` grabs a random problem from nationals part II\n" +
                                "`$query -s 0 -y 2005 -p 60`: problem #60 from 2005 USNCO Locals.\n" +
                                "`$query -s 2 -y 2010 -p 6`: problem #6 from 2010 USNCO Part II\n" +
                                "`$query -y 2010-2020 -p 25-30`: grabs a random kinetics question\n" +
                                "\tfrom the past 10 years.", null,
                        msg.getTimeCreated().plusSeconds(5), MESSAGE_COLOR, null, null,
                        AUTHOR_INFO, null, FOOTER, null, null);

                MessageChannel ch = msg.getChannel();
                ch.sendMessage(embed).queue();
            }
        }
    }

    private int getValue(int lowerBound, int upperBound) {
        int problemRange = upperBound - lowerBound + 1;

        return lowerBound + rng.nextInt(problemRange);
    }

    private void queryImage(Message msg, int actualYear, int actualProblem, int section) {
        // Query from GitHub
        String sectionVal;
        String sectionDescription;

        switch(section) {
            case 0:
                sectionVal = "locals";
                sectionDescription = "Locals";
                break;
            case 1:
                sectionVal = "part_i";
                sectionDescription = "Nationals Part I";
                break;
            default:
                sectionVal = "part_ii";
                sectionDescription = "Nationals Part II";
                break;
        }

        String url = "https://raw.githubusercontent.com/thewindsofwinter/usnco-problems/master/tests/"
                + sectionVal + "/" + actualYear + "/" + actualProblem + ".png";

        MessageEmbed.ImageInfo image = new MessageEmbed.ImageInfo(url, null, 1200, 673);

        MessageEmbed embed = new MessageEmbed(null, "*Problem #" + actualProblem + ", " + actualYear + " USNCO "
                + sectionDescription + "*",
                null, null,
                msg.getTimeCreated().plusSeconds(5), MESSAGE_COLOR, null, null,
                AUTHOR_INFO, null, FOOTER, image, null);

        MessageChannel ch = msg.getChannel();
        ch.sendMessage(embed).queue();
    }

    private void queryProblems(Message msg, String[] parameters) {
        // Bounds on the problem number and year number
        int lowerYear = 1999;
        int upperYear = 2020;
        boolean yearRestriction = false;

        int lowerProblem = 1;
        int upperProblem = 60;
        boolean problemRestriction = false;

        int section = -1;
        boolean partTwo = false;

        int currentOption = -1;

        for(int i = 1; i < parameters.length; i++) {
            String parameter = parameters[i];

            switch(currentOption) {
                case 0:
                    int[] years = setYearRestriction(msg, yearRestriction, parameter);

                    if (years[0] == ERROR_CODE[0]) {
                        return;
                    }

                    yearRestriction = true;
                    lowerYear = years[0];
                    upperYear = years[1];
                    currentOption = -1;
                    break;
                case 1:
                    int[] problems = setProblemRestriction(msg, problemRestriction, parameter, partTwo);

                    if (problems[0] == ERROR_CODE[0]) {
                        return;
                    }

                    problemRestriction = true;
                    lowerProblem = problems[0];
                    upperProblem = problems[1];
                    currentOption = -1;
                    break;
                case 2:
                    section = Integer.parseInt(parameter);
                    partTwo = (section == 2);
                    upperProblem = 8;
                    currentOption = -1;
                    break;
                default:
                    if (parameter.matches("-y(ear)?")) {
                        currentOption = 0;
                    } else if (parameter.matches("-p(roblem)?")) {
                        currentOption = 1;
                    } else if(parameter.matches("-s(ection)?")) {
                        currentOption = 2;
                    } else {
                        MessageChannel ch = msg.getChannel();
                        ch.sendMessage("`Error: Invalid option in query [not -s, -y, or -p]. Query terminated.`").queue();
                        return;
                    }
            }
        }

        // Assign section if none selected
        if(section == -1) {
            section = rng.nextInt(2);
        }

        int problem = getValue(lowerProblem, upperProblem);
        int year = getValue(lowerYear, upperYear);

        queryImage(msg, year, problem, section);
    }

    private void handleCommand(Message msg, String commands) {
        String[] tokens = commands.split(" ");
        String command = tokens[0];

        switch(command) {
            case "query":
                queryProblems(msg, tokens);
                break;
            case "help":
                printHelpMessage(msg, tokens);
                break;
            default:
                // Implement more commands later
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot())
            return;

        Message msg = event.getMessage();
        String content = msg.getContentRaw();

        if(content.charAt(0) == PREFIX) {
            handleCommand(msg, content.substring(1));
        }
    }
}
