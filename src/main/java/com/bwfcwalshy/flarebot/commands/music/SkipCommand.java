package com.bwfcwalshy.flarebot.commands.music;

import com.arsenarsen.lavaplayerbridge.PlayerManager;
import com.bwfcwalshy.flarebot.FlareBot;
import com.bwfcwalshy.flarebot.MessageUtils;
import com.bwfcwalshy.flarebot.commands.Command;
import com.bwfcwalshy.flarebot.commands.CommandType;
import com.bwfcwalshy.flarebot.scheduler.FlarebotTask;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SkipCommand implements Command {

    private PlayerManager musicManager;
    private Map<String, Map<String, Vote>> votes = new HashMap<>();
    private Map<String, Boolean> skips = new HashMap<>();

    public SkipCommand(FlareBot bot) {
        this.musicManager = bot.getMusicManager();
    }

    @Override
    public void onCommand(User sender, TextChannel channel, Message message, String[] args, Member member) {
        if (!channel.getGuild().getAudioManager().isConnected() ||
                musicManager.getPlayer(channel.getGuild().getId()).getPlayingTrack() == null) {
            MessageUtils.sendMessage("I am not streaming!", channel);
            return;
        }
        if (!channel.getGuild().getSelfMember().getVoiceState().getChannel().equals(member.getVoiceState().getChannel())) {
            MessageUtils.sendMessage("You must be in the channel in order to skip songs!", channel);
            return;
        }
        if (args.length != 1) {
            if (votes.containsKey(channel.getGuild().getId())) {
                String yes = String.valueOf(votes.get(channel.getGuild().getId()).values().stream().filter(vote -> vote == Vote.YES)
                        .count());
                String no = String.valueOf(votes.get(channel.getGuild().getId()).values().stream().filter(vote -> vote == Vote.NO)
                        .count());
                MessageUtils.sendMessage(MessageUtils.getEmbed(sender).setColor(new Color(229, 45, 39))
                        .setDescription("Can't start a vote right now! " +
                                "Another one in progress! Please use _skip YES|NO to vote!")
                        .addField("Votes for YES:", yes, true).addField("Votes for NO:", no, true), channel);
            } else getVotes(channel, member);
        } else {
            if (args[0].equalsIgnoreCase("force")) {
                if (getPermissions(channel).hasPermission(member, "flarebot.skip.force")) {
                    musicManager.getPlayer(channel.getGuild().getId()).skip();
                    skips.put(channel.getGuild().getId(), true);
                } else {
                    MessageUtils.sendMessage("You are missing the permission ``flarebot.skip.force`` which is required for use of this command!", channel
                    );
                }
                return;
            }
            Map<String, Vote> mvotes = getVotes(channel, member);
            if (mvotes == null)
                return;
            if (mvotes.containsKey(sender.getId())) {
                MessageUtils.sendMessage(MessageUtils.getEmbed(sender).setColor(new Color(229, 45, 39))
                        .setDescription("***\u26A0 You already voted! \u26A0***")
                        .addField("Your vote: ", mvotes.get(sender.getId()).toString(), true), channel);
                return;
            }
            try {
                Vote vote = Vote.valueOf(args[0].toUpperCase());
                mvotes.put(sender.getId(), vote);
            } catch (IllegalArgumentException e) {
                MessageUtils.sendMessage(MessageUtils.getEmbed(sender)
                        .setColor(new Color(229, 45, 39)).setDescription("***\u26A0 Use YES|NO! \u26A0***"), channel);
            }
        }
    }

    private Map<String, Vote> getVotes(TextChannel channel, Member sender) {
        return this.votes.computeIfAbsent(channel.getGuild().getId(), s -> {
            AtomicBoolean bool = new AtomicBoolean(false);
            channel.getGuild().getVoiceChannels().stream().filter(c -> c.equals(sender.getVoiceState().getChannel()))
                    .findFirst().ifPresent(c -> {
                if (c.getMembers().size() == 2
                        && c.equals(channel.getGuild().getSelfMember().getVoiceState().getChannel())) {
                    bool.set(true);
                    musicManager.getPlayer(s).skip();
                }
            });
            if (bool.get()) {
                MessageUtils.sendMessage(MessageUtils.getEmbed(sender.getUser())
                        .setDescription("You were the only person in the channel.\nSkipping!"), channel);
                return null;
            }
            new FlarebotTask("Vote " + s) {

                @Override
                public void run() {
                    if (skips.getOrDefault(s, false)) {
                        skips.remove(s);
                        votes.remove(s);
                        return;
                    }
                    String res = "";
                    boolean skip = votes.get(s).entrySet().stream()
                            .filter(e -> e.getValue() == Vote.YES)
                            .count() > (votes.size() / 2.0f);
                    MessageUtils.sendMessage(MessageUtils.getEmbed()
                            .setDescription("The votes are in!")
                            .addField("Results: ", (skip ? "Skip!" : "Keep!"), false), channel);
                    if (skip)
                        musicManager.getPlayer(s).skip();
                    votes.remove(s);
                }
            }.delay(20000);
            MessageUtils.sendMessage(MessageUtils.getEmbed(sender.getUser()).setDescription("The vote to skip **" +
                    musicManager.getPlayer(channel.getGuild().getId()).getPlayingTrack().getTrack().getInfo().title
                    + "** has started!\nUse _skip YES|NO to vote!"), channel);
            return new HashMap<>();
        });
    }

    @Override
    public String getCommand() {
        return "skip";
    }

    @Override
    public String getDescription() {
        return "Starts a skip voting, or if one is happening, marks a vote. `skip YES|NO` to pass a vote. To force skip use `skip force`";
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }

    @Override
    public String getPermission() {
        return "flarebot.skip";
    }

    private enum Vote {
        YES,
        NO;

        @Override
        public String toString() {
            return Character.toUpperCase(name().charAt(0)) + name().substring(1);
        }
    }
}
