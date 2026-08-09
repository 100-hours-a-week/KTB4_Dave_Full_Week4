package com.example.community.post.service;

import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;

public record PostDetailData(
        PostBodyData body,
        PostStateData state
) {
}
