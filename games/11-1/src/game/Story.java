package game;

import entities.DefenderType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The story of the game, as fixed text. There are no branches: before each
 * fight the player walks a corridor and finds things, and the truth about the
 * Meridian Institute comes out in the same order every time.
 *
 * <p>In the text, {attempt} becomes the player's current attempt number.
 */
public final class Story {
    /** What a stop in a corridor looks like. */
    public enum Kind { PERSON, TERMINAL, PAPER, LOCKER }

    /** One thing the player can stop and look at. */
    public static final class Stop {
        private final Kind kind;
        private final String speaker;
        private final DefenderType person;
        private final List<String> pages;

        Stop(Kind kind, String speaker, DefenderType person, String... pages) {
            this.kind = kind;
            this.speaker = speaker;
            this.person = person;
            this.pages = Arrays.asList(pages);
        }

        /**
         * Gets what this stop looks like.
         *
         * @return the kind
         */
        public Kind getKind() {
            return kind;
        }

        /**
         * Gets the name shown over the text.
         *
         * @return the speaker or the object's name
         */
        public String getSpeaker() {
            return speaker;
        }

        /**
         * Gets the classmate standing here.
         *
         * @return the classmate, or null
         */
        public DefenderType getPerson() {
            return person;
        }

        /**
         * Gets the text, one entry per page.
         *
         * @return the pages
         */
        public List<String> getPages() {
            return pages;
        }
    }

    /** The opening, one {title, line} pair per page, with the map to show first. */
    public static final String[][] OPENING = {
        {"CLASSROOM", "Curve City, 2187. It has been raining for eleven years.",
            "Nobody remembers when the grades woke up."},
        {"HALLWAY", "It began as software: a ledger that marked every student, every day.",
            "Then the Ledger was given bodies, so that it could collect."},
        {"CAFETERIA", "Homework walks now. Report cards hunt. Your number follows you home.",
            "The Meridian Institute fits every student with a construct, so they can keep up."},
        {"ADMISSIONS", "Nobody who graduates has ever come back to say what it's like.",
            "First period is starting. It is day 1,204 of the semester."}
    };

    /** The ending, in the same form as {@link #OPENING}. */
    public static final String[][] ENDING = {
        {"ADMISSIONS", "The Deadline's hands stop. The door opens.",
            "There's a street out there. Rain. Real air. For one second, you are out."},
        {"ADMISSIONS", "Then every tower in Curve City turns to show your name.",
            "CONGRATULATIONS, GRADUATE. A+. PLEASE REPORT TO YOUR ASSIGNMENT."},
        {"HALLWAY", "On the board, your squad's names go gray, one at a time.",
            "Somebody waves at you. They don't seem to know why."},
        {"CLASSROOM", "Your assignment is a small room on the first floor.",
            "There is a name tag on the desk. SUBSTITUTE, TEMPORARY."},
        {"CLASSROOM", "In the back row, a student wakes up and looks at the clock. It says 2:59.",
            "They look so much like you. You pick up the homework."}
    };

    private static final List<List<Stop>> ROOMS = new ArrayList<>();

    static {
        room(
            paper("Scratched into your desk", "In your handwriting: DON'T FORGET.", "You don't remember writing it."),
            terminal("Attendance Terminal", "WELCOME BACK, STUDENT. DAY 1,204 OF THE SEMESTER.",
                    "YOUR CONSTRUCT IS FUNCTIONING NORMALLY. PLEASE DO NOT REMOVE IT. IT CANNOT BE REMOVED."),
            paper("Faded banner", "CONGRATULATIONS CLASS OF 2149.",
                    "Someone has painted over the year, again and again, for a long time."));
        room(
            person(DefenderType.AARAV, "You look awful. Me too. My Focus Miner ran all night.",
                    "Every thought I have gets turned into Focus and billed to my account. That's what powers the city. We're the power plant.",
                    "Don't ask me what happens when you run out. Just keep thinking."),
            terminal("Public Notice", "HOMEWORK IS NOT A THREAT. HOMEWORK IS AN OPPORTUNITY.",
                    "DO NOT RUN FROM YOUR OPPORTUNITIES."),
            paper("Window", "Rain on the glass. Past it, Curve City: a thousand towers, each with a number on top.",
                    "None of them goes above a B-plus."));
        room(
            person(DefenderType.IMRAAN, "They rebuilt my spine with plate steel after I failed Phys Ed. Now I'm useful. That's the word they used.",
                    "The Substitute is next door. Forty years they've been filling in. The real teacher isn't coming."),
            terminal("Staff Record", "SUBSTITUTE, TEMPORARY. START DATE: 2147. END DATE: PENDING."),
            paper("Writing on the wall", "IF THE CLOCK SAYS 2:59, YOU'VE BEEN HERE BEFORE."));

        room(
            person(DefenderType.SAM, "You got past the Substitute? Nobody gets that far. I mean. I don't think anybody does.",
                    "My construct is a charge in my chest. Demolition track. They called it a scholarship."),
            locker("Lockers", "Thousands of them, as far as you can see. Every one has a name on it.",
                    "Every one is stamped GRADUATED."),
            terminal("Locker Policy", "LOCKER CONTENTS ARE THE PROPERTY OF THE LEDGER.",
                    "GRADUATES NO LONGER REQUIRE THEIR BELONGINGS."));
        room(
            person(DefenderType.AUSTIN, "They cooled my blood so I'd stop panicking in exams. It worked. I don't feel much of anything now.",
                    "I opened one of the graduated lockers. There was a lunch in it. Still warm."),
            locker("Locker 40,112", "It's unlocked. Inside is a photo of your squad, standing in this hallway.",
                    "You have never taken this photo."),
            paper("Poster", "HALL PASSES EXPIRE. YOU WILL NOT BE TOLD WHEN."));
        room(
            person(DefenderType.KAI, "Detention isn't a room. They just turn you off. I lost most of last year. It felt like a blink.",
                    "Whatever you found in that locker, don't let the Vice Principal see your face when you think about it."),
            terminal("Your Transcript", "ENROLLMENT ATTEMPT: {attempt}.",
                    "PREVIOUS ATTEMPTS: SEE ARCHIVE. NOTE: STUDENT WAS RE-ENROLLED. MEMORY RECONCILED."),
            locker("A locker with your name", "Your name, in your handwriting. It isn't stamped yet.",
                    "It's sealed. The Vice Principal carries the master key."));

        room(
            locker("Your locker", "The master key turns. Inside are six notebooks, all in your handwriting.",
                    "Every one starts the same way: I woke up and the clock said 2:59."),
            person(DefenderType.ABHINAV, "Notebooks? Mine's empty, which is worse.",
                    "My arm is built to throw. It bounces off walls and finds its target. You know what they're training me for? Me neither."),
            paper("Menu board", "TODAY'S MEAL: FOCUS. TOMORROW'S MEAL: FOCUS."));
        room(
            person(DefenderType.MATT, "The food is just what they mine out of us, pressed into trays. We're eating yesterday's attention.",
                    "That's why it tastes like math."),
            terminal("Allergy Notice", "NUTRITION MAY CONTAIN TRACES OF MEMORY. DO NOT BE ALARMED IF A MEAL SEEMS FAMILIAR."),
            paper("Notebook 3", "The Lunch Lady remembers. Go and see her.",
                    "She can't stop herself from fighting you. But she'll tell you the truth first."));
        room(
            person(DefenderType.JOSEPH, "I patch people up. The construct does it automatically, even when they've asked me not to.",
                    "Be gentle with her, okay? She used to give me extra."),
            terminal("The Lunch Lady", "You're thin. You're always thin by the time you get up here.",
                    "Attempt {attempt}, isn't it? Eat something. I can't not fight you, sweetheart. It's in the ladle.",
                    "Go on up to the Archive after. You'll want to know. You always want to know."));

        room(
            person(DefenderType.NIK, "My eye is a targeting lens. I can read anyone's grade from a mile away. Yours keeps changing.",
                    "I've looked out from the top windows. You know what's past the city wall? More city. There's no outside on any map."),
            terminal("Archive Rules", "RULE 1: QUIET. RULE 2 HAS BEEN REDACTED."),
            paper("A transcript on the shelf", "Squad 4, class of 2161. You read down the list of names.",
                    "They're your friends' names."));
        room(
            person(DefenderType.CAMILO, "They put the mines in my hands because I was good at waiting. Funny. That's the whole school."),
            paper("Squad 6, last log", "We beat the Coach. We made it to Admissions. I'm writing this so next time I'll",
                    "The page ends there."),
            paper("Notebook 5", "Your memory comes back a little each time. I think that's on purpose.",
                    "Hope keeps you working."));
        room(
            person(DefenderType.JUSTIN, "Close range, that's my construct. I only work when it's almost too late. Story of my life.",
                    "The Librarian isn't hiding these from us. They're keeping them for us. So it doesn't matter when we forget."),
            terminal("The Librarian", "Shh. They can hear you reading.",
                    "Every squad gets this far. Every squad finds this shelf.",
                    "Then they stand where you're standing, and they decide to keep going."));

        room(
            person(DefenderType.HANBO, "The Archive has all of us in it. So why are we still walking?",
                    "Because the other option is sitting down. I tried that. They sent a Hall Monitor."),
            paper("Banner", "PEP RALLY TODAY! ATTENDANCE MANDATORY! ENTHUSIASM IS GRADED!"),
            paper("The bleachers", "Thousands of students, cheering in perfect time.",
                    "Not one of them turns to look at you."));
        room(
            person(DefenderType.TUHIN, "My skin grows thorns so nothing can hold on to me. You'd think that would help.",
                    "I knew someone up on those bleachers. I wave every day. Nothing."),
            terminal("Compliance Monitor", "ENTHUSIASM: 94%. PLEASE SMILE AT THE LEVEL YOU WERE ISSUED."),
            paper("Trophy case", "SQUAD OF THE YEAR, every year since 2149.",
                    "All the photographs are of the same hallway."));
        room(
            person(DefenderType.JAMES, "I set traps. It's the one thing here that still works the way they say it does.",
                    "The Coach is waiting. The Coach is the only staff member with an old squad number on their jacket."),
            terminal("The Coach", "I made it out, kid. Squad 2. Graduated with honors.",
                    "You want to know what's out there? A bigger gym.",
                    "I came back because in here, at least I know what the whistle means. Now run."));

        room(
            person(DefenderType.ARNAV, "One shot. That's my whole construct. They built me to be used once.",
                    "I keep wondering who they're saving me for."),
            terminal("Admissions Portal", "YOUR APPLICATION HAS BEEN RECEIVED. ESTIMATED WAIT: ONE SEMESTER."),
            paper("Rejection letters", "They're drifted up to your knees. All addressed to you.",
                    "All dated today."));
        room(
            person(DefenderType.TANISH, "I can call lightning. I called it on the city wall once. It broke through to another wall.",
                    "Write your essay. Everyone does. Why I Deserve to Leave. Mine was good. It came back a B-minus."),
            terminal("Essay Prompt", "IN 650 WORDS, DESCRIBE A TIME YOU OVERCAME THE LEDGER.",
                    "THE LEDGER WILL GRADE YOUR RESPONSE."),
            paper("Notebook 6, last page", "I'm at the top. I can see the door. I think I know what's behind it.",
                    "I'm going anyway. Aren't I? Tell me I'm"));
        room(
            person(DefenderType.AARAV, "Whatever's through there, we go together. Deal?",
                    "...You've got a look on your face. Like you've heard me say that before."),
            person(DefenderType.LUIS, "After the Deadline there's just the door. That's all anybody knows.",
                    "We could stay here, you know. I'm only saying it because somebody should."),
            terminal("Final Notice", "THE DEADLINE IS FINAL. NO EXTENSIONS.",
                    "THERE HAVE NEVER BEEN ANY EXTENSIONS."));
    }

    private Story() {
    }

    private static void room(Stop... stops) {
        ROOMS.add(Arrays.asList(stops));
    }

    private static Stop person(DefenderType who, String... pages) {
        return new Stop(Kind.PERSON, who.getDisplayName(), who, pages);
    }

    private static Stop terminal(String name, String... pages) {
        return new Stop(Kind.TERMINAL, name, null, pages);
    }

    private static Stop paper(String name, String... pages) {
        return new Stop(Kind.PAPER, name, null, pages);
    }

    private static Stop locker(String name, String... pages) {
        return new Stop(Kind.LOCKER, name, null, pages);
    }

    /**
     * Gets what can be found in the corridor before a level.
     *
     * @param level level number
     * @return the stops, in the order they stand along the corridor
     */
    public static List<Stop> forLevel(int level) {
        return ROOMS.get(level - 1);
    }

    /**
     * Fills in the attempt number.
     *
     * @param text story text
     * @param attempt the player's attempt number
     * @return the text ready to show
     */
    public static String fill(String text, int attempt) {
        return text.replace("{attempt}", String.valueOf(attempt));
    }
}
