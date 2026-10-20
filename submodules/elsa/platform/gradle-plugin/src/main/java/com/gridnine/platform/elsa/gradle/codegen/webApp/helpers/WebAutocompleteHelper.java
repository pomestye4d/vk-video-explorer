/*
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.gridnine.platform.elsa.gradle.codegen.webApp.helpers;

import com.gridnine.platform.elsa.gradle.codegen.common.JavaCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.codegen.common.WebCodeGeneratorUtils;
import com.gridnine.platform.elsa.gradle.meta.webApp.AutocompleteWebElementDescription;

import java.io.File;
import java.io.IOException;

public class WebAutocompleteHelper {
    public static void generateAutocomplete(AutocompleteWebElementDescription descr, File destDir, String commonPackageName, String moduleName) throws IOException {
        var basicName = JavaCodeGeneratorUtils.getSimpleName(descr.getClassName());
        var skeletonName = "%sSkeleton".formatted(basicName);
        var skeletonImport = WebCodeGeneratorUtils.getImportName(descr.getClassName()+"Skeleton", commonPackageName, moduleName);
        var functionalComponentName = "%sFC".formatted(basicName);
        var componentName = "%sComponent".formatted(basicName);
        var result = """
                import { WebComponentWrapper } from '@/common/wrapper';
                import { initStateSetters } from '@/common/component';
                import {Option} from "%s/src-gen/common/Option";
                import { useEditor } from "%s/src/common/editor";
                import {
                    ChangeEvent,
                    KeyboardEvent,
                    useCallback,
                    useEffect,
                    useRef,
                    useState,
                } from 'react';
                import { %s } from '%s';
                
                function %s(props: { element: %s }) {
                          initStateSetters(props.element);
                          const editor = useEditor();
                          const [inputValue, setInputValue] = useState<string>('');
                          const [suggestions, setSuggestions] = useState<Option[]>([]);
                          const [selectedItems, setSelectedItems] = useState<Option[]>(
                              props.element.getValue()?.values || []
                          );
                          const [isOpen, setIsOpen] = useState<boolean>(false);
                          const [isLoading, setIsLoading] = useState<boolean>(false);
                          const [error, setError] = useState<string | null>(null);
                
                          const inputRef = useRef<HTMLInputElement>(null);
                          const timeoutRef = useRef<number | null>(null);
                          const abortControllerRef = useRef<AbortController | null>(null);
                          // Функция для проверки, выбран ли уже элемент
                          const isItemSelected = useCallback(
                              (item: Option): boolean => {
                                  return selectedItems.some((selectedItem) => selectedItem.id === item.id);
                              },
                              [selectedItems]
                          );
                
                          // Функция для запроса данных с сервера
                          const getSuggestions = useCallback(
                              async (query?: string) => {
                                  // Отменяем предыдущий запрос
                                  if (abortControllerRef.current) {
                                      abortControllerRef.current.abort();
                                  }
                
                                  abortControllerRef.current = new AbortController();
                
                                  setIsLoading(true);
                                  setError(null);
                                  const result = await props.element.doGetData({
                                      query,
                                      limit: props.element.getLimit(),
                                  });
                                  setIsLoading(false);
                                  setSuggestions(result.items);
                              },
                              [isItemSelected]
                          );
                
                          // Debounce функция
                          const debouncedFetch = useCallback(
                              (query: string): void => {
                                  if (timeoutRef.current !== null) {
                                      window.clearTimeout(timeoutRef.current);
                                  }
                
                                  timeoutRef.current = window.setTimeout(() => {
                                      setIsOpen(true);
                                      setIsLoading(true);
                                      getSuggestions(query);
                                  }, props.element.getDebounceTime());
                              },
                              [getSuggestions, props.element]
                          );
                
                          // Обработка ввода с debounce
                          const handleInputChange = useCallback(
                              (e: ChangeEvent<HTMLInputElement>): void => {
                                  if(editor) {
                                    editor.setHasChanges(true);
                                  }
                                  const value = e.target.value;
                                  setInputValue(value);
                                  debouncedFetch(value);
                              },
                              [debouncedFetch]
                          );
                
                          // Обработка выбора элемента
                          const handleSelect = (item: Option): void => {
                              if (!isItemSelected(item)) {
                                  const newSelectedItems = !!props.element.getMultiple()
                                      ? [...selectedItems, item]
                                      : [item];
                                  setSelectedItems(newSelectedItems);
                                  setInputValue('');
                                  setIsOpen(false);
                                  setSuggestions([]);
                                  props.element.setValidationMessage(undefined);
                
                                  // Колбэк при изменении выбранных элементов
                                  props.element.setValue(newSelectedItems);
                
                                  inputRef.current?.focus();
                              }
                          };
                
                          // Удаление выбранного элемента
                          const handleRemove = (itemToRemove: Option): void => {
                              const newSelectedItems = selectedItems.filter(
                                  (item) => item.id !== itemToRemove.id
                              );
                              setSelectedItems(newSelectedItems);
                
                              // Колбэк при изменении выбранных элементов
                              props.element.setValue(newSelectedItems);
                              props.element.setValidationMessage(undefined);
                          };
                
                          // Обработка клавиш
                          const handleKeyDown = useCallback(
                              (e: KeyboardEvent<HTMLInputElement>): void => {
                                  if (e.key === 'ArrowDown' && inputValue === '') {
                                      setIsOpen(true);
                                      setIsLoading(true);
                                      debouncedFetch('');
                                  } else if (
                                      e.key === 'Backspace' &&
                                      inputValue === '' &&
                                      selectedItems.length > 0
                                  ) {
                                      handleRemove(selectedItems[selectedItems.length - 1]);
                                  } else if (e.key === 'Escape') {
                                      setIsOpen(false);
                                  } else if (e.key === 'Enter' && suggestions.length > 0 && !isLoading) {
                                      // Выбираем первый элемент при нажатии Enter
                                      e.preventDefault();
                                      handleSelect(suggestions[0]);
                                  }
                              },
                              [inputValue, selectedItems, suggestions, isLoading, handleRemove, handleSelect]
                          );
                
                          // Очистка при размонтировании
                          useEffect(() => {
                              return () => {
                                  if (timeoutRef.current !== null) {
                                      window.clearTimeout(timeoutRef.current);
                                  }
                                  if (abortControllerRef.current) {
                                      abortControllerRef.current.abort();
                                  }
                              };
                          }, []);
                          return (
                              <WebComponentWrapper element={props.element}>
                                  <div
                                      className={`autocomplete-container${props.element.getValidationMessage() ? ' has-error' : ''}`}
                                  >
                                      {/* Выбранные элементы */}
                                      <div className="selected-items">
                                          {selectedItems.map((item) => (
                                              <span key={item.id} className="selected-item">
                                                  {item.displayName}
                                                  <button
                                                      type="button"
                                                      onClick={() => {
                                                          handleRemove(item);
                                                      }}
                                                      className="remove-btn"
                                                      aria-label={`Удалить ${item.displayName}`}
                                                  >
                                                      ×
                                                  </button>
                                              </span>
                                          ))}
                                      </div>
                
                                      {/* Поле ввода */}
                                      <div className="input-wrapper">
                                          <input
                                              name="autocomplete"
                                              ref={inputRef}
                                              type="text"
                                              value={inputValue}
                                              onChange={handleInputChange}
                                              onKeyDown={handleKeyDown}
                                              onFocus={() => setIsOpen(true)}
                                              className="autocomplete-input"
                                              disabled={isLoading}
                                              aria-expanded={isOpen}
                                              aria-haspopup="listbox"
                                              aria-controls="suggestions-list"
                                          />
                                          {isLoading && <div className="loading-indicator">⌛</div>}
                                      </div>
                
                                      {/* Выпадающий список */}
                                      {isOpen && (
                                          <div className="suggestions-container">
                                              {isLoading ? (
                                                  <div className="loading-message">Загрузка...</div>
                                              ) : error ? (
                                                  <div className="error-message">{error}</div>
                                              ) : suggestions.length > 0 ? (
                                                  <ul
                                                      id="suggestions-list"
                                                      className="suggestions-list"
                                                      role="listbox"
                                                      aria-label="Предложения"
                                                  >
                                                      {suggestions.map((item) => (
                                                          <li
                                                              key={item.id}
                                                              onClick={() => {
                                                                  handleSelect(item);
                                                              }}
                                                              className="suggestion-item"
                                                              role="option"
                                                              aria-selected={isItemSelected(item)}
                                                          >
                                                              {item.displayName}
                                                          </li>
                                                      ))}
                                                  </ul>
                                              ) : inputValue && !isLoading ? (
                                                  <div className="no-suggestions">
                                                      Ничего не найдено для "{inputValue}"
                                                  </div>
                                              ) : null}
                                          </div>
                                      )}
                                  </div>
                              </WebComponentWrapper>
                          );
                }
                
                export class %s extends %s {
                    functionalComponent = %s;
                
                    setValidationMessage(value?: string) {
                       this.stateSetters.get('validationMessage')!(value);
                    }
                }
                """.formatted(moduleName, moduleName, skeletonName, skeletonImport, functionalComponentName, componentName, componentName, skeletonName, functionalComponentName);
        var file = WebCodeGeneratorUtils.getFile(descr.getClassName() + ".tsx", destDir);
        if(!file.exists()) {
            WebCodeGeneratorUtils.saveIfDiffers(result, file);
        }
    }
}
