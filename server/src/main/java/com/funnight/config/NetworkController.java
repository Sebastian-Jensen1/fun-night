package com.funnight.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helps the host screen build a QR code friends can actually reach. When the game runs on a
 * laptop and is opened as "localhost", the link in the QR code has to use the laptop's wifi
 * address instead. Only answers requests from this machine, and can be switched off entirely.
 */
@RestController
@RequestMapping("/api/network")
public class NetworkController {

    public record NetworkInfo(List<String> lanAddresses) {
    }

    private final boolean enabled;

    public NetworkController(@Value("${funnight.lan-discovery:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @GetMapping
    public NetworkInfo network(HttpServletRequest request) {
        if (!enabled || !isLoopback(request.getRemoteAddr())) return new NetworkInfo(List.of());
        return new NetworkInfo(lanAddresses());
    }

    private static boolean isLoopback(String address) {
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> lanAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual() || nic.isPointToPoint()) continue;
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // no network information available: the client falls back to its own address
        }
        return addresses;
    }
}
