import { Pipe, PipeTransform } from '@angular/core';
import { ShowdownExtension } from 'showdown';
import { SafeHtml } from '@angular/platform-browser';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';

@Pipe({
    name: 'htmlForMarkdown',
})
export class HtmlForMarkdownPipe implements PipeTransform {
    constructor(private markdownService: ArtemisMarkdownService) {}

    /**
     * Converts markdown into html, sanitizes it and then declares it as safe to bypass further security.
     * @param {string} markdown the original markdown text
     * @param {ShowdownExtension[]} extensions to use for markdown parsing
     * @returns {string} the resulting html as a SafeHtml object that can be inserted into the angular template
     */
    transform(markdown: string, extensions: ShowdownExtension[] = []): SafeHtml | null {
        return this.markdownService.safeHtmlForMarkdown(markdown, extensions);
    }
}
