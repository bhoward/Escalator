/*
 * Simple Syntaxhighlighter brush for the output of Specs.
 * Author: Brian Howard
 * Date: 2010-06-12
 */
SyntaxHighlighter.brushes.Specs = function()
{
    this.regexList = [
        { regex: /^\s*\+.*$/gm, css: 'comments' }, // passed
        { regex: /^\s*x.*$/gm,  css: 'color3' },   // failed
        { regex: /^\s*o.*$/gm,  css: 'color1' },   // skipped
        { regex: /^\s*-.*$/gm,  css: 'string' }    // planned
        ];
};

SyntaxHighlighter.brushes.Specs.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.Specs.aliases = ['specs'];
