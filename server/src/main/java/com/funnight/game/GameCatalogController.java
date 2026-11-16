package com.funnight.game;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameCatalogController {

    /** Lists the six games in play order. */
    @GetMapping
    public List<GameInfo> listGames() {
        return Arrays.stream(GameType.values()).map(GameInfo::of).toList();
    }
}
