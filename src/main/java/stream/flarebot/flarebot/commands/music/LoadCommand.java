package stream.flarebot.flarebot.commands.music;

import stream.flarebot.flarebot.FlareBot;
import stream.flarebot.flarebot.commands.Command;
import stream.flarebot.flarebot.commands.CommandType;
import stream.flarebot.flarebot.music.VideoThread;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class LoadCommand implements Command {

    @Override
    public void onCommand(User sender, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {

            channel.sendMessage("Usage: " + FlareBot.getPrefix(channel.getGuild().getId()) + "load [NAME]").queue();
            return;
        }
        String name = FlareBot.getMessage(args, 0);

        channel.sendTyping().complete();

        VideoThread.getThread(name + '\u200B' + FlareBot.getInstance().getManager().loadPlaylist(channel, sender, name), channel, sender).start();
    }

    @Override
    public String getCommand() {
        return "load";
    }

    @Override
    public String getDescription() {
        return "Loads a playlist. Usage `load NAME`";
    }

    @Override
    public CommandType getType() {
        return CommandType.MUSIC;
    }

    @Override
    public String getPermission() {
        return "flarebot.playlist.load";
    }
}
