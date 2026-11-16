package dev.hoi.protocol.industry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndustryPayloadsTest {
    private static Gson json() throws ReflectiveOperationException {
        Field field = IndustryPayloads.class.getDeclaredField("JSON");
        field.setAccessible(true);
        return (Gson) field.get(null);
    }
    @Test void statsOmitOnlyDefaultFieldsAndReadBackExactly() throws Exception {
        var gson = json();
        var plain = new IndustryTankDesigner.Stat("armor_value", "£armor_texticon 장갑:", 31.0, false);
        var full = new IndustryTankDesigner.Stat("soft_attack", "대인 공격", 0.125, true,
                List.of(new IndustryTankDesigner.Contribution("포탑", 2.5)), -0.0, true);
        var changed = new IndustryTankDesigner.Stat("reliability", "신뢰도", 0.8, false, List.of(), -1.5, false);
        var type = new TypeToken<List<IndustryTankDesigner.Stat>>(){}.getType();
        String text = gson.toJson(List.of(plain, full, changed), type);
        assertEquals("[{\"id\":\"armor_value\",\"name\":\"£armor_texticon 장갑:\",\"value\":31.0},"
                + "{\"id\":\"soft_attack\",\"name\":\"대인 공격\",\"value\":0.125,\"percent\":true,\"contributions\":[{\"name\":\"포탑\",\"value\":2.5}],\"change\":-0.0,\"averaged\":true},"
                + "{\"id\":\"reliability\",\"name\":\"신뢰도\",\"value\":0.8,\"change\":-1.5}]", text);
        assertEquals(List.of(plain, full, changed), gson.fromJson(text, type));
        String old = new Gson().toJson(List.of(plain, full, changed), type);
        assertEquals(List.of(plain, full, changed), gson.fromJson(old, type));
        assertEquals(List.of(plain, full, changed), new Gson().fromJson(text, type));
    }
}
