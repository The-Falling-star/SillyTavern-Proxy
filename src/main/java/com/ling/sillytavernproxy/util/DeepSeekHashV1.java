package com.ling.sillytavernproxy.util;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ling.sillytavernproxy.config.FinalNumber;
import io.github.kawamuray.wasmtime.*;
import io.github.kawamuray.wasmtime.Module;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;

@Slf4j
public class DeepSeekHashV1 {
    
    private static final String WASM_PATH = "../../../../../resources/sha3_wasm_bg.7b9ca65ddd.wasm";
    private static Memory memory;
    private static Func allocate; //__wbindgen_export_0
    private static final int WASM_PAGE_SIZE = 65536; // WASM 内存页大小

    public static Integer computePowAnswer(String algorithm, String challengeStr, String salt, int difficulty, long expireAt) throws IOException {

        if (!"DeepSeekHashV1".equals(algorithm)) throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        String prefix = salt + "_" + expireAt + "_";

        // 1. 加载 WASM 模块
        Path wasmFile = Paths.get(WASM_PATH);
        byte[] wasmBytes = Files.readAllBytes(wasmFile);

        try (Engine engine = new Engine();
             Store<Void> store = new Store<>(null,engine);
             Module module = new Module(engine, wasmBytes);
             Linker linker = new Linker(engine)) {

            // 获取外部依赖项
            ArrayList<Extern> externals = new ArrayList<>();
            linker.externs(store).forEach(externItem -> externals.add(externItem.extern()));

            // 创建 Instance
            Instance instance = new Instance(store, module, externals);

            // 2. 获取 WASM 导出函数
            memory = instance.getMemory(store, "memory").orElseThrow();
            Func addToStack = instance.getFunc(store, "__wbindgen_add_to_stack_pointer").get();
            allocate = instance.getFunc(store, "__wbindgen_export_0").get();
            //__wbindgen_export_1
            Func reallocate = instance.getFunc(store, "__wbindgen_export_1").get();
            Func wasmSolve = instance.getFunc(store, "wasm_solve").get();

            // 3. 定义辅助函数（与 WASM 内存交互）
            //没有直接操作内存地址的方法, 通过Buffer实现
            ByteBuffer memoryBuffer = memory.buffer(store);

            //java是大端, wasm是小端
            memoryBuffer.order(ByteOrder.LITTLE_ENDIAN);

            // 4. 准备参数并调用 WASM 函数
            // 4.1 申请栈空间
            int retPtr = addToStack.call(store, Val.fromI32(-16))[0].i32();

            // 4.2 编码 challenge 和 prefix
            int ptrChallenge = encodeString(store, challengeStr);
            int lenChallenge = challengeStr.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            int ptrPrefix = encodeString(store, prefix);
            int lenPrefix = prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;


            // 4.3 调用 wasm_solve (difficulty 需要转换为 float)
            wasmSolve.call(store,
                    Val.fromI32(retPtr),
                    Val.fromI32(ptrChallenge),
                    Val.fromI32(lenChallenge),
                    Val.fromI32(ptrPrefix),
                    Val.fromI32(lenPrefix),
                    Val.fromF64(difficulty));

            // 5. 读取结果
            // 5.1 读取状态
            memoryBuffer.position(retPtr);
            int status = memoryBuffer.getInt();

            //5.2 读取值
            long valueLong = memoryBuffer.getLong(retPtr + 8);  //直接读取8个字节

            // 6. 恢复栈指针
            addToStack.call(store, Val.fromI32(16));

            // 7. 返回结果
            return status == 0 ? null : (int) Double.longBitsToDouble(valueLong);

        } catch (WasmtimeException e) {
            throw new RuntimeException("Error during WASM execution", e);
        }
    }

    private static int encodeString(Store<Void> store, String str) {
        byte[] encoded = str.getBytes();
        int length = encoded.length;
        int ptr = allocate.call(store, Val.fromI32(length), Val.fromI32(1))[0].i32();
        ByteBuffer memoryBuffer = memory.buffer(store);
        // 确保有足够的容量
        if (memoryBuffer.capacity() < ptr + length) {
            memory.grow(store,(ptr + length - memoryBuffer.capacity() + WASM_PAGE_SIZE - 1) / WASM_PAGE_SIZE); // 向上取整
            memoryBuffer = memory.buffer(store); // 重新获取buffer
        }

        memoryBuffer.position(ptr);
        memoryBuffer.put(encoded);
        return ptr;
    }

    public String getAnswer(String challenge, String salt, String signature, int difficulty, long expireAt) throws IOException {
        // 计算答案
        Integer answer = computePowAnswer(FinalNumber.DEEPSEEK_ALGORITHM, challenge, salt, difficulty, expireAt);
        if(answer != null) log.info("找到答案:{}",answer);
        else log.info("找不到答案");

        // 构建 JSON 对象
        JSONObject powResponse = new JSONObject();
        powResponse.set("algorithm", FinalNumber.DEEPSEEK_ALGORITHM);
        powResponse.set("challenge", challenge);
        powResponse.set("salt", salt);
        powResponse.set("answer", answer);
        powResponse.set("signature", signature);
        powResponse.set("target_path", FinalNumber.DEEPSEEK_TARGET_PATH);

        // 按照顺序来Base64编码
        return Base64.getEncoder().encodeToString(JSONUtil.toJsonStr(powResponse).getBytes(StandardCharsets.UTF_8));
    }

}
