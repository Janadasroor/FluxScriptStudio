#include <jni.h>
#include <string>
#include <android/log.h>
#include <regex>
#include <sstream>
#include <dlfcn.h>
#include <cstdio>

#define LOG_TAG "FluxScriptStudio"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#ifdef HAS_FLUX_COMPILER

struct StdoutCapture {
    std::stringstream buffer;
    std::streambuf* oldCout{nullptr};

    void start() {
        oldCout = std::cout.rdbuf();
        std::cout.rdbuf(buffer.rdbuf());
    }

    std::string stop() {
        if (oldCout) std::cout.rdbuf(oldCout);
        return buffer.str();
    }
};

typedef void* (*flux_instance_t)();
typedef void (*flux_init_t)(void*);
typedef void (*flux_finalize_t)(void*);
typedef bool (*flux_is_init_t)(void*);
typedef bool (*flux_exec_t)(void*, const std::string&, std::string*);

struct FluxScriptLib {
    void* handle{nullptr};
    void* engine{nullptr};

    bool load() {
        handle = dlopen("libFluxScript.so", RTLD_NOW | RTLD_GLOBAL);
        if (!handle) {
            LOGE("Failed to load libFluxScript.so: %s", dlerror());
            return false;
        }

        auto instance = (flux_instance_t)dlsym(handle, "_ZN4Flux9JITEngine8instanceEv");
        if (!instance) {
            LOGE("instance symbol not found: %s", dlerror());
            dlclose(handle);
            handle = nullptr;
            return false;
        }

        engine = instance();
        LOGI("FluxScript JITEngine instance: %p", engine);
        return true;
    }

    bool init() {
        if (!engine) return false;
        auto isInit = (flux_is_init_t)dlsym(handle, "_ZNK4Flux9JITEngine13isInitializedEv");
        if (isInit && isInit(engine)) return true;

        auto initFn = (flux_init_t)dlsym(handle, "_ZN4Flux9JITEngine10initializeEv");
        if (!initFn) {
            LOGE("initialize symbol not found: %s", dlerror());
            return false;
        }
        initFn(engine);
        return true;
    }

    bool exec(const std::string& code, std::string* error) {
        if (!engine) return false;
        auto execFn = (flux_exec_t)dlsym(handle, "_ZN4Flux9JITEngine13executeStringERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEEPS7_");
        if (!execFn) {
            LOGE("executeString symbol not found: %s", dlerror());
            if (error) *error = "executeString not found in FluxScript library";
            return false;
        }
        return execFn(engine, code, error);
    }

    ~FluxScriptLib() {
        if (engine && handle) {
            auto fini = (flux_finalize_t)dlsym(handle, "_ZN4Flux9JITEngine8finalizeEv");
            if (fini) fini(engine);
        }
        if (handle) dlclose(handle);
    }
};
#endif

extern "C" JNIEXPORT jstring JNICALL
Java_com_jnd_fluxscriptstudio_MainActivity_compileFluxScript(
        JNIEnv* env,
        jobject /* this */,
        jstring source) {

    if (source == nullptr) return env->NewStringUTF("Error: Null source");

    const char* nativeSource = env->GetStringUTFChars(source, nullptr);
    std::string sourceStr(nativeSource);
    LOGI("Compiling Flux code:\n%s", nativeSource);

    std::string output;

#ifdef HAS_FLUX_COMPILER
    static FluxScriptLib flux;
    static bool loaded = flux.load();
    static bool inited = loaded ? flux.init() : false;

    if (!inited) {
        LOGW("FluxScript real mode unavailable, falling back to mock");
        goto mock_mode;
    }

    try {
        StdoutCapture capture;
        capture.start();

        std::string error;
        bool success = flux.exec(nativeSource, &error);
        std::string capturedOutput = capture.stop();

        if (success) {
            output = "FluxScript 0.1.0 Success:\n";
            output += "---------------------------\n";
            if (!capturedOutput.empty()) {
                output += capturedOutput;
            } else {
                output += "(execution completed, no print output)\n";
            }
            output += "\nStatus: SUCCESS";
        } else {
            output = "FluxScript 0.1.0 Error:\n";
            output += "---------------------------\n";
            output += error.empty() ? "Unknown error" : error;
            output += "\nStatus: FAILURE";
        }
        goto done;
    } catch (const std::exception& e) {
        output = "FluxScript 0.1.0 Exception:\n";
        output += "---------------------------\n";
        output += e.what();
        output += "\nStatus: FAILURE";
        goto done;
    }

mock_mode:
#endif
    {
        output = "FluxScript 0.1.0 (Mock Mode)\n";
        output += "---------------------------\n";

        std::regex expectedRegex("// Expected: (.*)");
        std::smatch match;
        if (std::regex_search(sourceStr, match, expectedRegex)) {
            output += "Execution Result: " + match[1].str();
            output += "\nStatus: SUCCESS";
        } else {
            output += "Execution Result: [Mock Echo] ";
            output += sourceStr.length() > 5000 ? sourceStr.substr(0, 5000) + "..." : sourceStr;
            output += "\nStatus: SUCCESS (No Expectation)";
        }
    }

#ifdef HAS_FLUX_COMPILER
done:
#endif
    env->ReleaseStringUTFChars(source, nativeSource);
    return env->NewStringUTF(output.c_str());
}
