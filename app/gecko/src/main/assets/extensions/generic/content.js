console.log("Browkorf TV generic content extension loaded");

//prevent text selection for the whole page (run only once)
window.addEventListener('load', function () {
    //document.body.style.userSelect = "none";
    console.log("window.load executed");
});

const communicatePort = browser.runtime.connectNative("browkorftv_content");

communicatePort.onMessage.addListener(message => {
    switch (message.action) {
        case "updateSelection": {
            let x = message.data.x;
            let y = message.data.y;
            let w = message.data.width;
            let h = message.data.height;
            let pageX = x * (window.innerWidth / window.visualViewport.scale / w) + window.visualViewport.offsetLeft;
            let pageY = y * (window.innerHeight / window.visualViewport.scale / h) + window.visualViewport.offsetTop;

            if (document.caretPositionFromPoint) {
                let caretPosition = document.caretPositionFromPoint(pageX, pageY);
                let node = caretPosition.offsetNode;
                let offset = caretPosition.offset;
                let selection = window.getSelection();
                if (selection.anchorNode === null || selection.anchorNode === undefined) {
                    selection.setPosition(node, offset);
                } else {
                    selection.extend(node, offset);
                }
            }
            break;
        }

        case "clearSelection": {
            let selection = window.getSelection();
            selection.removeAllRanges();
            break;
        }

        case "processSelection": {
            let selection = window.getSelection();
            let selectedText = selection.toString();
            let editable = false;
            if (selection.anchorNode) {
                let node = selection.anchorNode;
                while (node) {
                    if (node.isContentEditable) {
                        editable = true;
                        break;
                    }
                    node = node.parentNode;
                }
            }
            let data = {
                selectedText: selectedText,
                editable: editable
            };
            communicatePort.postMessage({ action: "selectionProcessed", data: data });
            break;
        }

        case "replaceSelection": {
            let selection = window.getSelection();
            let replacement = message.data;
            let range = selection.getRangeAt(0);
            range.deleteContents();
            range.insertNode(document.createTextNode(replacement));
            break;
        }
    }
});