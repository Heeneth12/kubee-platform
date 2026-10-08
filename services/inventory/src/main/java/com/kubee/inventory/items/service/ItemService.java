package com.kubee.inventory.items.service;

import com.kubee.inventory.items.dto.ItemDto;
import com.kubee.inventory.items.dto.ItemFilterDto;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface ItemService {

    CommonResponse createItem(ItemDto itemDto);
    CommonResponse updateItem(Long id, ItemDto itemDto);
    ItemDto getItemById(Long id);
    Page<ItemDto> getAllItems(Integer page, Integer size, ItemFilterDto itemFilterDto);
    CommonResponse toggleItemActiveStatus(Long id, Boolean active);
    List<ItemDto> itemSearch(ItemFilterDto itemFilter) throws CommonException;
    CommonResponse<?> saveBulkItems(MultipartFile file) throws CommonException;
    ByteArrayInputStream loadItemsForDownload(ItemFilterDto itemFilter) throws CommonException;
    ByteArrayInputStream getItemTemplate() throws CommonException;

}
