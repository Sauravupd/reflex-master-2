package com.sauravupd.twitchbubbles

import kotlin.random.Random

object Messages {

    private val MOCK_MESSAGES = listOf(
        "Are your thumbs made of stone?",
        "A snail just lapped your reflex time.",
        "I've seen dial-up modems react faster.",
        "Did you fall asleep mid-game?",
        "Reflexes of a very tired potato.",
        "Maybe try using your eyes next time?",
        "That was... impressively slow.",
        "Are you playing in slow motion?",
        "I've seen statues with better reaction times.",
        "The bubbles aren't even trying to hide.",
        "Is your screen covered in butter?",
        "My grandma pops bubbles faster than that.",
        "You're making the bubbles look like speedsters.",
        "Are you sure you're awake?",
        "That performance was a cinematic 24fps.",
        "Reflex level: Glacial.",
        "You're lagging in real life.",
        "Did you forget to tap?",
        "Error 404: Reflexes not found.",
        "You react like a loading bar at 99%.",
        "Is the gravity too strong for your thumbs?",
        "The bubbles are laughing at you.",
        "Try drinking some coffee. A lot of coffee.",
        "That was a valiant effort... for a sloth.",
        "Are you playing with mittens on?",
        "You're giving the bubbles a false sense of security.",
        "Reaction time measured in business days.",
        "Maybe turn the phone the right way up?",
        "I've seen continental drift happen faster.",
        "Your thumbs are having a mid-life crisis.",
        "Did you blink for the entire game?",
        "You're making 'easy' look 'impossible'.",
        "The bubbles aren't scared of you.",
        "Reflexes: Some assembly required.",
        "Are you using your elbows to play?",
        "A brick has more twitch potential.",
        "You're playing at the speed of a 'stop' sign.",
        "Is your brain on power-saver mode?",
        "That was a masterclass in missing.",
        "Bubbles 1, Human 0.",
        "Try not to be so distracted by the colors.",
        "Your reaction time is a work in progress.",
        "Are you waiting for the bubbles to ask permission?",
        "You missed that like it was your job.",
        "Reflexes: Coming soon to a player near you.",
        "That was almost a score. Almost.",
        "You move with the grace of a falling anvil.",
        "Are your thumbs on strike?",
        "I've seen tectonic plates move faster.",
        "Better luck next century."
    )

    private val PRAISE_MESSAGES = listOf(
        "Are you part machine?",
        "God-tier reflexes detected.",
        "The Matrix called, they want their Chosen One back.",
        "Your thumbs are a blur.",
        "Absolute twitch mastery.",
        "Neural link established. You are one with the screen.",
        "Is that you, Flash?",
        "Reflexes of a striking cobra.",
        "You're popping bubbles in the fourth dimension.",
        "Sub-atomic reaction speeds reached.",
        "Your score is making the CPU sweat.",
        "Legendary performance, Pilot.",
        "Are you overclocking your brain?",
        "The bubbles never stood a chance.",
        "Total annihilation of the bubble menace.",
        "You're seeing the code now, aren't you?",
        "Elite status confirmed.",
        "That was pure, unfiltered skill.",
        "Your reaction time is basically zero.",
        "Flawless execution.",
        "You've transcended human limitations.",
        "The bubbles are terrified of your name.",
        "Masterful coordination.",
        "You're playing in God Mode.",
        "Is there a limit to your speed?",
        "Incredible focus.",
        "You just redefined the word 'Twitch'.",
        "Absolutely surgical precision.",
        "You're moving faster than the refresh rate.",
        "A symphony of perfect taps.",
        "The AI is genuinely impressed.",
        "That was a reflex clinic.",
        "You're making this look easy.",
        "The bubbles are begging for mercy.",
        "Speed beyond comprehension.",
        "You have the heart of a champion.",
        "Unstoppable momentum.",
        "Your thumbs are literal lightning bolts.",
        "A perfect display of agility.",
        "You just set the bar too high.",
        "Are you using some kind of neural lace?",
        "Simply majestic popping.",
        "You're in the zone. Don't leave.",
        "Twitch reflexes: MAXIMUM.",
        "You're a bubble-popping deity.",
        "The screen can barely keep up.",
        "Pure adrenaline, pure skill.",
        "You've mastered the art of the pop.",
        "Absolute unit of a player.",
        "Reflexes: Perfection."
    )

    /**
     * Returns a random message based on whether the player did well.
     * "Good" threshold: score >= 100 (roughly 10+ pops with combos)
     */
    fun getGameOverMessage(score: Int, level: Int): String {
        val didWell = score >= 100 || level >= 3
        return if (didWell) {
            PRAISE_MESSAGES[Random.nextInt(PRAISE_MESSAGES.size)]
        } else {
            MOCK_MESSAGES[Random.nextInt(MOCK_MESSAGES.size)]
        }
    }
}
