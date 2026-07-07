{ pkgs, lib, ... }:

{
  packages = [];

  languages.java = {
    enable = true;
    jdk.package = pkgs.zulu21;
  };

  env = {
    MOD_VERSION = "0.0.0";
    GRADLE_USER_HOME = "./.gradle";
    LD_LIBRARY_PATH = with pkgs; lib.makeLibraryPath [
      libGL
      glfw
      openal
      flite
      libpulseaudio
      udev
      libxcursor
      libxxf86vm
      libxrandr
      libxext
      libx11
    ];
  };
}
