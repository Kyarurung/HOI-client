package dev.hoi.client.ui;

import com.google.gson.Gson;
import dev.hoi.protocol.EquipmentDesignPreset;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class EquipmentDesignPresets {
    private static final Gson JSON=new Gson();
    private record FileData(int version,List<EquipmentDesignPreset> presets) {}
    private final Path file;
    public EquipmentDesignPresets(Path file){this.file=file;}
    public List<EquipmentDesignPreset> load() throws IOException {
        if(!Files.exists(file))return List.of();
        if(Files.size(file)>2*1024*1024)throw new IOException("저장된 설계 파일이 너무 큽니다.");
        try {
            var data=JSON.fromJson(Files.readString(file),FileData.class);
            if(data==null||data.version()!=1||data.presets()==null||data.presets().size()>128)throw new IllegalArgumentException();
            var keys=new HashSet<String>();
            for(var preset:data.presets()) {
                Objects.requireNonNull(preset);
                if(!keys.add(preset.chassis()+"\n"+preset.name()))throw new IllegalArgumentException();
            }
            return List.copyOf(data.presets());
        } catch(RuntimeException error){throw new IOException("저장된 설계 파일을 읽을 수 없습니다. 기존 파일을 확인하세요.",error);}
    }
    public void save(EquipmentDesignPreset preset) throws IOException {
        var values=new ArrayList<>(load());
        values.removeIf(p->p.chassis().equals(preset.chassis())&&p.name().equals(preset.name()));
        if(values.size()>=128)throw new IOException("저장된 설계는 최대 128개입니다.");
        values.add(preset);
        values.sort(Comparator.comparing(EquipmentDesignPreset::chassis).thenComparing(EquipmentDesignPreset::name));
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary=Files.createTempFile(file.toAbsolutePath().getParent(),"equipment-presets-",".tmp");
        try {
            Files.writeString(temporary,JSON.toJson(new FileData(1,values)));
            try{Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException error){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
    }
}
