import com.google.gson.GsonBuilder;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelGenerator;
import dev.simulated_team.simulated.content.blocks.directional_gearshift.DirectionalGearshiftGenerator;
import dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlockStateGen;
import java.nio.file.*;

/** Command-line data generation from genuine production native blockstate definitions. */
public final class GenerateNativeModelStates {
    public static void main(String[] args)throws Exception{
        var directory=Path.of(args[0]);Files.createDirectories(directory);var json=new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(directory.resolve("steering_wheel.json"),json.toJson(SteeringWheelGenerator.generate())+"\n");
        Files.writeString(directory.resolve("directional_gearshift.json"),json.toJson(DirectionalGearshiftGenerator.generate())+"\n");
        Files.writeString(directory.resolve("redstone_accumulator.json"),json.toJson(RedstoneAccumulatorBlockStateGen.generate())+"\n");
    }
}
