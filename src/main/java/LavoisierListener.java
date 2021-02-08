import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class LavoisierListener extends ListenerAdapter {

    // Dumb global constants
    // Random token below, not mine
    private static final long CREATOR = 8952138908519L;
    private static final String BOT_VERSION = "Lavoisier v0.3";
    private static final String ICON_URL = "https://cdn.discordapp.com/attachments/536299955178962965/771746782190764032/science-2652279_1280.png";
    private static final String CSV_URL = "https://raw.githubusercontent.com/thewindsofwinter/usnco-problems/master/SolutionTable.csv";
    private static final char PREFIX = '$';
    private static final int MESSAGE_COLOR = new Color(0, 200, 200).getRGB();
    private static final MessageEmbed.Footer FOOTER = new MessageEmbed.Footer(BOT_VERSION, ICON_URL, null);
    private static final MessageEmbed.AuthorInfo AUTHOR_INFO = new MessageEmbed.AuthorInfo("Lavoisier - a bot for all things USNCO", null, null, null);
    private Random rng = new Random();
    private static final int[] ERROR_CODE = {-1, -1};

    private HashMap<Integer, String> data = new HashMap<>();
    private JDA instance;

    private int lowerYear = 1999;
    private int upperYear = 2020;
    private boolean yearRestriction = false;

    private int lowerProblem = 1;
    private int upperProblem = 60;
    private boolean problemRestriction = false;
    private boolean partTwo = false;

    private String[] header;

    /**
     * A constructor that adds a reference to the parent JDA instance
     *
     * @param jda the instance of the JDA used to get users
     */
    LavoisierListener(JDA jda) {
        instance = jda;
    }

    /**
     * Load answer data into program
     */
    private void loadData() {
        try {
            URL source = new URL(CSV_URL);
            URLConnection connect = source.openConnection();
            BufferedReader f = new BufferedReader(new InputStreamReader(connect.getInputStream()));

            header = f.readLine().split(",");
            // Replace third header with Difficulty Rating
            header[3] = "Difficulty";

            String line;
            while((line = f.readLine()) != null) {
                String[] row = line.split(",");
                // Section: 0, 1, 2, 3 for now, 3 will be WCC

                int problemNumber = Integer.parseInt(row[0]);
                int section;
                int year;
                String exam = row[1];
                String answer = row[2];

                if(row.length > 3) {
                    answer += "~" + row[3];
                }

                if(exam.matches("USNCO .*")) {
                    exam = exam.substring(6);
                    year = Integer.parseInt(exam.substring(1, 5));
                    if(exam.charAt(0) == 'N') {
                        section = 1;
                    }
                    else {
                        section = 0;
                    }
                }
                else {
                    year = Integer.parseInt(exam.substring(4, 8));
                    section = 3;
                }

                int id = year*240 + problemNumber*4 + section;
                data.put(id, answer);
            }
        }
        catch (Exception e) {
            // Solution given by StackOverflow: https://stackoverflow.com/questions/1149703/
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString().substring(0, 800); // stack trace as a string

            // Send error messages to me
            instance.retrieveUserById(CREATOR).queue(
                    user -> user.openPrivateChannel().queue(
                            channel -> channel.sendMessage(sStackTrace).queue()));

        }
    }

    /**
     * A method to set a restriction on the years queried
     *
     * @param msg the overall message sent
     * @param parameter the parameter to set years
     *
     * @return a pair of years, lower and upper, or an error code {-1, -1}
     */
    private int[] setYearRestriction(Message msg, String parameter) {
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
            int lower = Integer.parseInt(years[0]);
            int upper = Integer.parseInt(years[0]);

            if(years.length > 1) {
                upper = Integer.parseInt(years[1]);
            }

            if(lower > upper) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [year range invalid]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            if(lower < 1999 || upper > 2020) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [years not between 1999-2020]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            return new int[]{lower, upper};
        }
    }

    /**
     * A method to set a restriction on the problems queried
     *
     * @param msg the overall message sent
     * @param parameter the parameter to set problem bounds
     *
     * @return a pair of problems, lower and upper, or an error code {-1, -1}
     */
    private int[] setProblemRestriction(Message msg, String parameter) {
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
            int lower = Integer.parseInt(problems[0]);
            int upper = Integer.parseInt(problems[0]);

            if(problems.length > 1) {
                upper = Integer.parseInt(problems[1]);
            }

            if(lower > upper) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [problem range invalid]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            if(lower < 1 || upper > maxProblem) {
                MessageChannel ch = msg.getChannel();
                ch.sendMessage("`Error: Invalid query [problem not between 1-" + maxProblem + "]. Query terminated.`").queue();
                return ERROR_CODE;
            }

            return new int[]{lower, upper};
        }
    }

    /**
     * A method to print out the help message
     *
     * @param msg The overall message sent
     * @param parameters The parameters on the help message
     */
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

    /**
     * A method to get a random value within a range
     *
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     *
     * @return a random integer within the given range
     */
    private int getValue(int lowerBound, int upperBound) {
        int problemRange = upperBound - lowerBound + 1;

        return lowerBound + rng.nextInt(problemRange);
    }

    /**
     * A method to query a USNCO problem
     *
     * @param msg the original message
     * @param actualYear the year of the problem
     * @param actualProblem the problem number of the problem
     * @param section the test the problem was on (Locals, Part I, or Part II)
     */
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

        List<MessageEmbed.Field> fields = new ArrayList<>();
        int problemID = actualYear*240 + actualProblem*4 + section;

        fields.add(new MessageEmbed.Field(header[1], actualYear + " USNCO "
                + sectionDescription, false));
        fields.add(new MessageEmbed.Field(header[0], "" + actualProblem, true));

        if(data.containsKey(problemID)) {
            String[] info = data.get(problemID).split("~");

            fields.add(new MessageEmbed.Field(header[2], "||" + info[0] + "||", true));

            if(info.length < 2) {
                fields.add(new MessageEmbed.Field(header[3], "Unrated", true));
            }
            else {
                double percent = Double.parseDouble(info[1].substring(0, info[1].length() - 1));
                if(percent > 66.6) {
                    fields.add(new MessageEmbed.Field(header[3], "Easy (>66%)", true));
                }
                else if(percent > 50.0) {
                    fields.add(new MessageEmbed.Field(header[3], "Normal (>50%)", true));
                }
                else if(percent > 33.3) {
                    fields.add(new MessageEmbed.Field(header[3], "Hard (<50%)", true));
                }
                else {
                    fields.add(new MessageEmbed.Field(header[3], "Insane (<33%)", true));
                }
            }
        }

        String url = "https://raw.githubusercontent.com/thewindsofwinter/usnco-problems/master/tests/"
                + sectionVal + "/" + actualYear + "/" + actualProblem + ".png";

        MessageEmbed.ImageInfo image = new MessageEmbed.ImageInfo(url, null, 1200, 673);

        MessageEmbed embed = new MessageEmbed(null, null,
                null, null,
                msg.getTimeCreated().plusSeconds(5), MESSAGE_COLOR, null, null,
                AUTHOR_INFO, null, FOOTER, image, fields);

        MessageChannel ch = msg.getChannel();
        ch.sendMessage(embed).queue();
    }

    /**
     * A method to reset year, problem, and restriction parameters to their original value
     */
    private void resetParams() {
        lowerYear = 1999;
        upperYear = 2020;
        yearRestriction = false;

        lowerProblem = 1;
        upperProblem = 60;
        problemRestriction = false;
        partTwo = false;
    }

    /**
     * A method to restrict the year range problems are queried from
     *
     * @param msg the original message sent
     * @param parameter the parameter included in the message
     * @return 0 if the method was successful, -1 if there was an error
     */
    private int restrictYear(Message msg, String parameter) {
        int[] years = setYearRestriction(msg, parameter);

        if (years[0] == ERROR_CODE[0]) {
            return -1;
        }

        yearRestriction = true;
        lowerYear = years[0];
        upperYear = years[1];
        return 0;
    }

    /**
     * A method to restrict the problem range problems are queried from
     *
     * @param msg the original message sent
     * @param parameter the parameter included in the message
     * @return 0 if the method was successful, -1 if there was an error
     */
    private int restrictProblem(Message msg, String parameter) {
        int[] problems = setProblemRestriction(msg, parameter);

        if (problems[0] == ERROR_CODE[0]) {
            return -1;
        }

        problemRestriction = true;
        lowerProblem = problems[0];
        upperProblem = problems[1];
        return 0;
    }

    /**
     * A method to restrict the test (local, part I, part II) our problems are queried from
     *
     * @param parameter a parameter for problem query
     * @return the restricted section
     */
    private int restrictSection(String parameter) {
        int sec = Integer.parseInt(parameter);

        if(sec == 2) {
            partTwo = true;
            upperProblem = 8;
        }

        return sec;
    }

    /**
     * A method to query a problem based on user input
     *
     * @param msg the user's message
     * @param parameters the parameters in the message
     */
    private void queryProblems(Message msg, String[] parameters) {
        if(data.isEmpty())
            loadData();

        // Bounds on the problem number and year number
        resetParams();

        int section = -1;
        int currentOption = -1;

        int success = 0;
        for(int i = 1; i < parameters.length; i++) {
            if(success == -1)
                return;

            String parameter = parameters[i];

            switch(currentOption) {
                case 0:
                    success = restrictYear(msg, parameter);
                    currentOption = -1;
                    break;
                case 1:
                    success = restrictProblem(msg, parameter);
                    currentOption = -1;
                    break;
                case 2:
                    section = restrictSection(parameter);
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


    /**
     * A method to handle commands overall
     *
     * @param msg the original message of the user
     * @param commands the content of the user command
     */
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

    /**
     * Handle message sending and receiving
     *
     * @param event the event when the message is received
     */
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
