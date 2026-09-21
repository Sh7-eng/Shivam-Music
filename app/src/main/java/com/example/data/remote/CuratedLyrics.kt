package com.example.data.remote

object CuratedLyrics {

    fun getCuratedLyrics(trackName: String, artistName: String): String? {
        val t = trackName.lowercase()
        val a = artistName.lowercase()

        return when {
            t.contains("blinding lights") || t.contains("blinding") -> BLINDING_LIGHTS_LRC
            t.contains("shape of you") -> SHAPE_OF_YOU_LRC
            t.contains("starboy") -> STARBOY_LRC
            t.contains("levitating") -> LEVITATING_LRC
            t.contains("kesariya") -> KESARIYA_LRC
            t.contains("stay") && (a.contains("laroi") || a.contains("bieber")) -> STAY_LRC
            else -> null
        }
    }

    private val BLINDING_LIGHTS_LRC = """
[00:00.00] ♪ (Intro Synthesizer) ♪
[00:13.20]Yeah
[00:16.40]I've been tryna call
[00:18.90]I've been on my own for long enough
[00:22.40]Maybe you can show me how to love, maybe
[00:28.80]I'm going through withdrawals
[00:32.10]You don't even have to do too much
[00:35.30]You can turn me on with just a touch, baby
[00:41.20]I look around and Sin City's cold and empty
[00:46.80]No one's around to judge me
[00:50.10]I can't see clearly when you're gone
[00:54.00]I said, ooh, I'm blinded by the lights
[01:00.50]No, I can't sleep until I feel your touch
[01:07.10]I said, ooh, I'm drowning in the night
[01:13.60]Oh, when I'm like this, you're the one I trust
[01:19.40]Hey, hey, hey
[01:21.00]I'm running out of time
[01:24.40]'Cause I can see the sun light up the sky
[01:27.70]So I hit the road in overdrive, baby, oh
[01:34.20]The city's cold and empty
[01:38.90]No one's around to judge me
[01:42.50]I can't see clearly when you're gone
[01:46.40]I said, ooh, I'm blinded by the lights
[01:52.90]No, I can't sleep until I feel your touch
[01:59.40]I said, ooh, I'm drowning in the night
[02:06.00]Oh, when I'm like this, you're the one I trust
[02:13.50]I'm just walking by to let you know
[02:18.00]I can never say it on the phone
[02:22.20]Will never let you go this time
[02:25.70]I said, ooh, I'm blinded by the lights
[02:32.20]No, I can't sleep until I feel your touch
[02:39.50]Hey, hey, hey
[02:45.00]I said, ooh, I'm blinded by the lights
[02:51.50]No, I can't sleep until I feel your touch
[02:58.00]♪ (Outro Synth) ♪
    """.trimIndent()

    private val SHAPE_OF_YOU_LRC = """
[00:00.00] ♪ (Intro Marimba) ♪
[00:09.10]The club isn't the best place to find a lover
[00:11.80]So the bar is where I go
[00:13.90]Me and my friends at the table doing shots
[00:16.40]Drinking fast and then we talk slow
[00:18.70]Come over and start up a conversation with just me
[00:21.50]And trust me, I'll give it a chance now
[00:23.70]Take my hand, stop, put Van the Man on the jukebox
[00:26.50]And then we start to dance, and now I'm singing like
[00:29.00]Girl, you know I want your love
[00:31.20]Your love was handmade for somebody like me
[00:33.80]Come on now, follow my lead
[00:36.00]I may be crazy, don't mind me
[00:38.60]Say, boy, let's not talk too much
[00:40.90]Grab on my waist and put that body on me
[00:43.60]Come on now, follow my lead
[00:45.70]Come, come on now, follow my lead
[00:48.40]I'm in love with the shape of you
[00:50.80]We push and pull like a magnet do
[00:53.30]Although my heart is falling too
[00:55.70]I'm in love with your body
[00:58.10]And last night you were in my room
[01:00.60]And now my bedsheets smell like you
[01:03.00]Every day discovering something brand new
[01:05.40]I'm in love with your body
[01:07.80]Oh—I—oh—I—oh—I—oh—I
[01:10.30]I'm in love with your body
[01:12.70]Oh—I—oh—I—oh—I—oh—I
[01:15.20]I'm in love with your body
[01:17.60]Oh—I—oh—I—oh—I—oh—I
[01:20.10]I'm in love with your body
[01:22.50]Every day discovering something brand new
[01:24.90]I'm in love with the shape of you
    """.trimIndent()

    private val STARBOY_LRC = """
[00:00.00] ♪ (Intro Beat) ♪
[00:10.50]I'm tryna put you in the worst mood, ah
[00:13.20]P1 cleaner than your church shoes, ah
[00:15.90]Milli point two just to hurt you, ah
[00:18.50]All red Lamb' just to tease you, ah
[00:21.20]None of these toys on lease too, ah
[00:23.90]Made your whole year in a week too, yah
[00:26.50]Main bitch out your league too, ah
[00:29.10]Side bitch out of your league too, ah
[00:31.80]House so empty, need a centerpiece
[00:34.50]Twenty racks a table, cut from ebony
[00:37.20]Cut that ivory into skinny pieces
[00:39.80]Then she clean it with her face, man, I love my baby
[00:42.50]You talkin' money, need a hearing aid
[00:45.10]You talkin' 'bout me, I don't see the shade
[00:47.80]Switch up my style, I take any lane
[00:50.40]I switch up my cup, I kill any pain
[00:53.10]Look what you've done
[00:57.00]I'm a motherfuckin' starboy
[01:03.70]Look what you've done
[01:07.70]I'm a motherfuckin' starboy
    """.trimIndent()

    private val LEVITATING_LRC = """
[00:00.00] ♪ (Intro Disco Bass) ♪
[00:08.50]If you wanna run away with me, I know a galaxy
[00:12.00]And I can take you for a ride
[00:16.80]I had a premonition that we fell into a rhythm
[00:20.70]Where the music don't stop for life
[00:25.20]Glitter in the sky, glitter in my eyes
[00:29.40]Shining just the way I like
[00:33.70]If you're feeling like you need a little bit of company
[00:38.20]You met me at the perfect time
[00:42.10]You want me, I want you, baby
[00:46.30]My sugarboo, I'm levitating
[00:50.60]The Milky Way, we're renegading
[00:54.80]Yeah-yeah-yeah-yeah
[00:59.00]I got you, moonlight, you're my starlight
[01:03.20]I need you all night, come on, dance with me
[01:07.50]I'm levitating
[01:11.80]You, moonlight, you're my starlight
[01:16.00]I need you all night, come on, dance with me
[01:20.30]I'm levitating
    """.trimIndent()

    private val KESARIYA_LRC = """
[00:00.00] ♪ (Flute & Acoustic Guitar Intro) ♪
[00:15.20]Mujhko itna bataye koyi
[00:20.40]Kaise tujhse dil na lagaye koyi
[00:25.70]Rabba ne tujhko banane mein
[00:30.90]Kardi hai husn ki khaali tijoriyan
[00:36.20]Kaajal ki siyahi se likhi
[00:41.40]Hai tune jaane kitno ki love storiyan
[00:46.70]Kesariya tera ishq hai piya
[00:54.50]Rang jaaun jo main haath lagaun
[01:02.40]Din beete saara teri fikr mein
[01:10.30]Rain saari teri khair manaun
[01:18.20]Kesariya tera ishq hai piya
[01:26.10]Rang jaaun jo main haath lagaun
[01:34.00]Din beete saara teri fikr mein
[01:41.80]Rain saari teri khair manaun
    """.trimIndent()

    private val STAY_LRC = """
[00:00.00] ♪ (Intro Synthesizer) ♪
[00:06.20]I do the same thing I told you that I never would
[00:09.50]I told you I'd change, even when I knew I never could
[00:13.20]I know that I can't find nobody else as good as you
[00:16.70]I need you to stay, need you to stay, hey
[00:20.10]I get drunk, wake up, I'm wasted still
[00:23.20]I realize the time that I wasted here
[00:26.70]I feel like you can't feel the way I feel
[00:30.00]Oh, I'll be fucked up if you can't be right here
[00:33.40]Oh, ooh-woah (oh, ooh-woah, ooh-woah)
[00:37.00]Oh, ooh-woah (oh, ooh-woah, ooh-woah)
[00:40.40]Oh, ooh-woah (oh, ooh-woah, ooh-woah)
[00:43.70]Oh, I'll be fucked up if you can't be right here
    """.trimIndent()
}
